package com.example.scheduler.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.dto.appointment.RescheduleAppointmentRequest;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.DoctorAvailability;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.ScheduleException;
import com.example.scheduler.entity.Specialty;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.ApiException;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.AppointmentMapper;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.AppointmentNotificationType;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.DoctorAvailabilityRepository;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import com.example.scheduler.tenant.TenantContext;
import com.example.scheduler.websocket.event.AppointmentChangedEvent;
import com.example.scheduler.websocket.event.BoardEvent;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    private static final Long CLINIC_ID = 7L;
    private static final Long DOCTOR_ID = 10L;
    private static final Long OTHER_DOCTOR_ID = 11L;
    private static final Long PATIENT_ID = 20L;
    private static final Long APPOINTMENT_ID = 30L;
    private static final Long SAVED_ID = 99L;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);

    private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final LocalDateTime FUTURE_START = LocalDateTime.of(2026, 11, 10, 9, 0);

    private static final DayOfWeek FUTURE_DAY = FUTURE_START.getDayOfWeek();

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private ScheduleExceptionRepository scheduleExceptionRepository;

    @Mock
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Mock
    private CurrentClinicProvider currentClinicProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MessageSource messageSource;

    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(CLINIC_ID, "demo");
        service = new AppointmentServiceImpl(
                appointmentRepository,
                patientRepository,
                personalRepository,
                appointmentMapper,
                scheduleExceptionRepository,
                doctorAvailabilityRepository,
                currentClinicProvider,
                eventPublisher,
                messageSource,
                CLOCK);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Appointment givenExisting(AppointmentStatus status, LocalDateTime start) {
        var existing =
                appointment(APPOINTMENT_ID, doctor(DOCTOR_ID, ERole.DOCTOR), patient(PATIENT_ID, true), start, status);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(existing));
        return existing;
    }

    private void givenDoctorLocked(Personal doctor) {
        when(personalRepository.findByIdForUpdate(doctor.getId())).thenReturn(Optional.of(doctor));
    }

    private void givenWorkingDay(Long doctorId, DayOfWeek day) {
        when(doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, day))
                .thenReturn(List.of(availability(day, 8, 12, 30)));
    }

    private void givenPatient(Patient patient) {
        when(patientRepository.findByIdForShare(patient.getId())).thenReturn(Optional.of(patient));
    }

    private void givenExistingActive(Appointment existing) {
        when(appointmentRepository.findFirstByPatientIdAndDoctorIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
                        anyLong(), anyLong(), any(), any()))
                .thenReturn(Optional.of(existing));
    }

    private void givenSaveAssignsId() {
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            if (a.getId() == null) a.setId(SAVED_ID);
            return a;
        });
    }

    private AppointmentResponse givenMapperResponse() {
        var response = new AppointmentResponse(
                SAVED_ID, FUTURE_START, null, DOCTOR_ID, null, null, null, PATIENT_ID, null, null, null, null, null);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(response);
        return response;
    }

    private void givenClinic() {
        when(currentClinicProvider.getCurrentClinic())
                .thenReturn(Clinic.builder()
                        .id(CLINIC_ID)
                        .name("Clinica Demo")
                        .slug("demo")
                        .build());
    }

    private Appointment captureSaved() {
        var captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        return captor.getValue();
    }

    private List<Object> publishedEvents() {
        return mockingDetails(eventPublisher).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("publishEvent"))
                .map(i -> i.getArgument(0))
                .toList();
    }

    private List<AppointmentNotificationEvent> notifications() {
        return publishedEvents().stream()
                .filter(AppointmentNotificationEvent.class::isInstance)
                .map(AppointmentNotificationEvent.class::cast)
                .toList();
    }

    private AppointmentNotificationEvent singleNotification() {
        var notifications = notifications();
        assertThat(notifications).hasSize(1);
        return notifications.getFirst();
    }

    private static <T extends ApiException> T assertApiException(
            ThrowingCallable call, Class<T> type, String messageKey, Object... args) {
        var thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(type);
        var ex = type.cast(thrown);
        assertThat(ex.getMessageKey()).isEqualTo(messageKey);
        if (args.length > 0) assertThat(ex.getArgs()).containsExactly(args);
        return ex;
    }

    private static AppointmentRequest request(Long doctorId, LocalDateTime start) {
        return new AppointmentRequest(doctorId, PATIENT_ID, start);
    }

    private static RescheduleAppointmentRequest reschedule(LocalDateTime start) {
        return new RescheduleAppointmentRequest(start);
    }

    private static Personal doctor(Long id, ERole role) {
        return Personal.builder()
                .id(id)
                .name("Dr " + id)
                .role(role)
                .specialty(Specialty.builder().id(1L).name("Cardiologia").build())
                .build();
    }

    private static Patient patient(Long id, boolean active) {
        return Patient.builder()
                .id(id)
                .name("Patient " + id)
                .email("p" + id + "@mail.test")
                .phoneNumber("+591700000" + id)
                .active(active)
                .build();
    }

    private static Appointment appointment(
            Long id, Personal doctor, Patient patient, LocalDateTime start, AppointmentStatus status) {
        return Appointment.builder()
                .id(id)
                .doctor(doctor)
                .patient(patient)
                .startTime(start)
                .endTime(start.plusMinutes(30))
                .status(status)
                .build();
    }

    private static DoctorAvailability availability(DayOfWeek day, int fromHour, int toHour, int slotMinutes) {
        return DoctorAvailability.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(fromHour, 0))
                .endTime(LocalTime.of(toHour, 0))
                .slotDurationMinutes(slotMinutes)
                .build();
    }

    private static ScheduleException partialBlock(LocalTime start, LocalTime end) {
        return ScheduleException.builder()
                .isFullDayBlock(false)
                .startTime(start)
                .endTime(end)
                .build();
    }

    @Nested
    @DisplayName("bookAppointment (staff)")
    class BookAppointment {
        @Test
        void shouldSavePendingAppointmentAndPublishBoardEventWhenSlotIsFree() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            var patient = patient(PATIENT_ID, true);
            givenDoctorLocked(doctor);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient);
            givenSaveAssignsId();
            var response = givenMapperResponse();

            var result = service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false);

            assertThat(result).isSameAs(response);
            var saved = captureSaved();
            assertThat(saved.getDoctor()).isSameAs(doctor);
            assertThat(saved.getPatient()).isSameAs(patient);
            assertThat(saved.getStartTime()).isEqualTo(FUTURE_START);
            assertThat(saved.getEndTime()).isEqualTo(FUTURE_START.plusMinutes(30));
            assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(saved.getCreatedAt()).isEqualTo(NOW);
            assertThat(publishedEvents())
                    .containsExactly(new AppointmentChangedEvent(
                            CLINIC_ID, DOCTOR_ID, new BoardEvent(SAVED_ID, AppointmentStatus.PENDING)));
        }

        @Test
        void shouldCheckOverlapWithoutExclusionAndIgnoringCancelledWhenBooking() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();

            service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false);

            verify(appointmentRepository)
                    .existsOverlappingAppointment(
                            DOCTOR_ID,
                            FUTURE_START,
                            FUTURE_START.plusMinutes(30),
                            List.of(AppointmentStatus.CANCELLED),
                            null);
        }

        @Test
        void shouldThrowForbiddenWhenRestrictedDoctorBooksForAnotherDoctor() {
            assertApiException(
                    () -> service.bookAppointment(request(OTHER_DOCTOR_ID, FUTURE_START), DOCTOR_ID, true),
                    ForbiddenException.class,
                    MessageKeys.APPOINTMENT_UNAUTHORIZED);

            verifyNoInteractions(personalRepository, appointmentRepository);
        }

        @Test
        void shouldBookWhenRestrictedDoctorBooksForItself() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();

            service.bookAppointment(request(DOCTOR_ID, FUTURE_START), DOCTOR_ID, true);

            verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        void shouldBookWhenTargetIsAdministratorThatIsAlsoDoctor() {
            var adminDoctor = doctor(DOCTOR_ID, ERole.ADMINISTRATOR);
            adminDoctor.setAlsoDoctor(true);
            givenDoctorLocked(adminDoctor);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();

            service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false);

            assertThat(captureSaved().getDoctor()).isSameAs(adminDoctor);
        }

        @Test
        void shouldThrowNotFoundWhenDoctorDoesNotExist() {
            when(personalRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.empty());

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    ResourceNotFoundException.class,
                    MessageKeys.PERSONAL_DOCTOR_NOT_FOUND,
                    DOCTOR_ID);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowBadRequestWhenTargetIsAssistant() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.ASSISTANT));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BadRequestException.class,
                    MessageKeys.PERSONAL_DOCTOR_NOT_FOUND,
                    DOCTOR_ID);
        }

        @Test
        void shouldThrowBadRequestWhenTargetIsAdministratorThatIsNotDoctor() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.ADMINISTRATOR));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BadRequestException.class,
                    MessageKeys.PERSONAL_DOCTOR_NOT_FOUND,
                    DOCTOR_ID);
        }

        @Test
        void shouldThrowConflictWhenDoctorIsInactive() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            doctor.setActive(false);
            givenDoctorLocked(doctor);

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_DOCTOR_INACTIVE);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowBadRequestWhenStartIsInThePast() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, NOW.minusMinutes(1)), 1L, false),
                    BadRequestException.class,
                    MessageKeys.APPOINTMENT_PAST_DATE);
        }

        @Test
        void shouldBookWhenStartIsExactlyNow() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, NOW.getDayOfWeek());
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();

            service.bookAppointment(request(DOCTOR_ID, NOW), 1L, false);

            assertThat(captureSaved().getStartTime()).isEqualTo(NOW);
        }

        @Test
        void shouldThrowOutOfJourneyWhenDoctorHasNoAvailabilityThatDay() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_OUT_OF_JOURNEY);
        }

        @Test
        void shouldThrowOutOfJourneyWhenStartIsNotAlignedToSlot() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START.withMinute(10)), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_OUT_OF_JOURNEY);
        }

        @Test
        void shouldThrowOutOfJourneyWhenStartIsAtAvailabilityEnd() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START.withHour(12)), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_OUT_OF_JOURNEY);
        }

        @Test
        void shouldThrowOutOfJourneyWhenStartIsBeforeAvailabilityStart() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START.withHour(7)), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_OUT_OF_JOURNEY);
        }

        @Test
        void shouldIgnoreAvailabilityWithoutSlotDurationWhenFindingSlot() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            when(doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(DOCTOR_ID, FUTURE_DAY))
                    .thenReturn(List.of(availability(FUTURE_DAY, 8, 12, 0)));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_OUT_OF_JOURNEY);
        }

        @Test
        void shouldBookLastSlotWhenItEndsExactlyAtAvailabilityEnd() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();
            var lastSlot = FUTURE_START.withHour(11).withMinute(30);

            service.bookAppointment(request(DOCTOR_ID, lastSlot), 1L, false);

            assertThat(captureSaved().getEndTime()).isEqualTo(lastSlot.plusMinutes(30));
        }

        @Test
        void shouldThrowScheduleBlockedWhenDayIsFullyBlocked() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, FUTURE_START.toLocalDate()))
                    .thenReturn(List.of(
                            ScheduleException.builder().isFullDayBlock(true).build()));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_SCHEDULE_BLOCKED);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldBookWhenPartialBlockEndsExactlyAtSlotStart() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, FUTURE_START.toLocalDate()))
                    .thenReturn(List.of(partialBlock(LocalTime.of(8, 0), LocalTime.of(9, 0))));
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();

            service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false);

            verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        void shouldThrowScheduleBlockedWhenPartialBlockOverlapsSlot() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, FUTURE_START.toLocalDate()))
                    .thenReturn(List.of(partialBlock(LocalTime.of(9, 15), LocalTime.of(10, 0))));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_SCHEDULE_BLOCKED);
        }

        @Test
        void shouldThrowSlotTakenWhenOverlappingAppointmentExists() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            when(appointmentRepository.existsOverlappingAppointment(
                            eq(DOCTOR_ID), eq(FUTURE_START), any(), any(), isNull()))
                    .thenReturn(true);

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_SLOT_TAKEN);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowNotFoundWhenPatientDoesNotExist() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            when(patientRepository.findByIdForShare(PATIENT_ID)).thenReturn(Optional.empty());

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    ResourceNotFoundException.class,
                    MessageKeys.PATIENT_NOT_FOUND,
                    PATIENT_ID);
        }

        @Test
        void shouldThrowConflictWhenPatientIsInactive() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient(PATIENT_ID, false));

            assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_PATIENT_INACTIVE);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldLookUpUpcomingActiveAppointmentOfPatientWithSameDoctor() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();

            service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false);

            verify(appointmentRepository)
                    .findFirstByPatientIdAndDoctorIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
                            PATIENT_ID,
                            DOCTOR_ID,
                            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED),
                            NOW);
        }

        @Test
        @DisplayName("should reject with patientHasActive, status choice 0 and full details when existing is PENDING")
        void shouldThrowPatientHasActiveWithDetailsWhenPatientHasPendingAppointmentWithDoctor() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            givenDoctorLocked(doctor);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            var patient = patient(PATIENT_ID, true);
            givenPatient(patient);
            var existingStart = LocalDateTime.of(2026, 11, 15, 14, 30);
            givenExistingActive(appointment(555L, doctor, patient, existingStart, AppointmentStatus.PENDING));

            var ex = assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE,
                    "15/11/2026",
                    "14:30",
                    0);

            assertThat(ex.getDetails())
                    .containsExactly(
                            entry("startTime", existingStart),
                            entry("appointmentId", 555L),
                            entry("status", AppointmentStatus.PENDING));
            verify(appointmentRepository, never()).save(any());
            assertThat(publishedEvents()).isEmpty();
        }

        @Test
        void shouldUseConfirmedChoiceWhenExistingActiveAppointmentIsConfirmed() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            givenDoctorLocked(doctor);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            var patient = patient(PATIENT_ID, true);
            givenPatient(patient);
            var existingStart = LocalDateTime.of(2026, 12, 1, 8, 5);
            givenExistingActive(appointment(556L, doctor, patient, existingStart, AppointmentStatus.CONFIRMED));

            var ex = assertApiException(
                    () -> service.bookAppointment(request(DOCTOR_ID, FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE,
                    "01/12/2026",
                    "08:05",
                    1);

            assertThat(ex.getStatus().value()).isEqualTo(409);
            assertThat(ex.getDetails()).containsEntry("status", AppointmentStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("bookGuestAppointment")
    class BookGuestAppointment {
        @Test
        void shouldSavePendingAppointmentWhenGuestBooksFreeSlot() {
            givenDoctorLocked(doctor(DOCTOR_ID, ERole.DOCTOR));
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenPatient(patient(PATIENT_ID, true));
            givenSaveAssignsId();
            var response = givenMapperResponse();

            var result = service.bookGuestAppointment(request(DOCTOR_ID, FUTURE_START));

            assertThat(result).isSameAs(response);
            assertThat(captureSaved().getStatus()).isEqualTo(AppointmentStatus.PENDING);
        }

        @Test
        @DisplayName(
                "should reject with guest key and only the start time when guest already has an active appointment")
        void shouldThrowGuestConflictWithoutAppointmentStateWhenPatientHasActiveAppointment() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            givenDoctorLocked(doctor);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            var patient = patient(PATIENT_ID, true);
            givenPatient(patient);
            var existingStart = LocalDateTime.of(2026, 11, 15, 14, 30);
            givenExistingActive(appointment(555L, doctor, patient, existingStart, AppointmentStatus.CONFIRMED));

            var ex = assertApiException(
                    () -> service.bookGuestAppointment(request(DOCTOR_ID, FUTURE_START)),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE_GUEST,
                    "15/11/2026",
                    "14:30");

            assertThat(ex.getDetails()).containsExactly(entry("startTime", existingStart));
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowConflictWhenGuestBooksInactiveDoctor() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            doctor.setActive(false);
            givenDoctorLocked(doctor);

            assertApiException(
                    () -> service.bookGuestAppointment(request(DOCTOR_ID, FUTURE_START)),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_DOCTOR_INACTIVE);
        }
    }

    @Nested
    @DisplayName("rescheduleAppointmentById")
    class RescheduleAppointment {
        private static final LocalDateTime CURRENT_START = LocalDateTime.of(2026, 11, 9, 9, 0);

        @Test
        void shouldThrowNotFoundWhenAppointmentDoesNotExist() {
            when(appointmentRepository.findDoctorIdById(APPOINTMENT_ID)).thenReturn(Optional.empty());

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false),
                    ResourceNotFoundException.class,
                    MessageKeys.APPOINTMENT_NOT_FOUND,
                    APPOINTMENT_ID);
            verifyNoInteractions(personalRepository);
        }

        @Test
        void shouldThrowNotFoundWhenDoctorCannotBeLocked() {
            when(appointmentRepository.findDoctorIdById(APPOINTMENT_ID)).thenReturn(Optional.of(DOCTOR_ID));
            when(personalRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.empty());

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false),
                    ResourceNotFoundException.class,
                    MessageKeys.PERSONAL_DOCTOR_NOT_FOUND,
                    DOCTOR_ID);
        }

        @Test
        void shouldLockDoctorBeforeLoadingAppointment() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            givenExistingForReschedule(doctor, AppointmentStatus.PENDING);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenSaveAssignsId();

            service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false);

            var order = inOrder(appointmentRepository, personalRepository);
            order.verify(appointmentRepository).findDoctorIdById(APPOINTMENT_ID);
            order.verify(personalRepository).findByIdForUpdate(DOCTOR_ID);
            order.verify(appointmentRepository).findById(APPOINTMENT_ID);
        }

        @Test
        void shouldThrowForbiddenWhenRestrictedDoctorReschedulesAnotherDoctorsAppointment() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.PENDING);

            assertApiException(
                    () -> service.rescheduleAppointmentById(
                            APPOINTMENT_ID, reschedule(FUTURE_START), OTHER_DOCTOR_ID, true),
                    ForbiddenException.class,
                    MessageKeys.APPOINTMENT_UNAUTHORIZED);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldRescheduleWhenRestrictedDoctorOwnsAppointment() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.PENDING);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenSaveAssignsId();

            service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), DOCTOR_ID, true);

            assertThat(captureSaved().getStartTime()).isEqualTo(FUTURE_START);
        }

        @Test
        void shouldThrowRescheduleOnlyActiveWhenAppointmentIsCancelled() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.CANCELLED);

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_RESCHEDULE_ONLY_ACTIVE);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowPastNotModifiableWhenAppointmentAlreadyStarted() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            givenLockedDoctorFor(doctor);
            var started = appointment(
                    APPOINTMENT_ID, doctor, patient(PATIENT_ID, true), NOW.minusMinutes(1), AppointmentStatus.PENDING);
            when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(started));

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_PAST_NOT_MODIFIABLE);
        }

        @Test
        void shouldThrowSameTimeWhenNewStartEqualsCurrentStart() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.CONFIRMED);

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(CURRENT_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_RESCHEDULE_SAME_TIME);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowPastDateWhenNewStartIsInThePast() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.PENDING);

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(NOW.minusHours(1)), 1L, false),
                    BadRequestException.class,
                    MessageKeys.APPOINTMENT_PAST_DATE);
        }

        @Test
        void shouldThrowDoctorInactiveWhenDoctorWasDeactivated() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            doctor.setActive(false);
            givenExistingForReschedule(doctor, AppointmentStatus.PENDING);

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_DOCTOR_INACTIVE);
        }

        @Test
        void shouldThrowSlotTakenAndExcludeItselfFromOverlapCheck() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.PENDING);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            when(appointmentRepository.existsOverlappingAppointment(
                            DOCTOR_ID,
                            FUTURE_START,
                            FUTURE_START.plusMinutes(30),
                            List.of(AppointmentStatus.CANCELLED),
                            APPOINTMENT_ID))
                    .thenReturn(true);

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_SLOT_TAKEN);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowOutOfJourneyWhenNewStartIsOutsideAvailability() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.PENDING);

            assertApiException(
                    () -> service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_OUT_OF_JOURNEY);
        }

        @Test
        void shouldMovePendingAppointmentWithoutNotifyingPatient() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.PENDING);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenSaveAssignsId();
            var response = givenMapperResponse();

            var result = service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false);

            assertThat(result).isSameAs(response);
            var saved = captureSaved();
            assertThat(saved.getStartTime()).isEqualTo(FUTURE_START);
            assertThat(saved.getEndTime()).isEqualTo(FUTURE_START.plusMinutes(30));
            assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(publishedEvents())
                    .containsExactly(new AppointmentChangedEvent(
                            CLINIC_ID, DOCTOR_ID, new BoardEvent(APPOINTMENT_ID, AppointmentStatus.PENDING)));
            verifyNoInteractions(currentClinicProvider);
        }

        @Test
        void shouldReturnConfirmedAppointmentToPendingAndPublishRescheduledNotification() {
            givenExistingForReschedule(doctor(DOCTOR_ID, ERole.DOCTOR), AppointmentStatus.CONFIRMED);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenSaveAssignsId();
            givenClinic();

            service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), 1L, false);

            assertThat(captureSaved().getStatus()).isEqualTo(AppointmentStatus.PENDING);
            var notification = singleNotification();
            assertThat(notification.type()).isEqualTo(AppointmentNotificationType.RESCHEDULED);
            assertThat(notification.status()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(notification.appointmentId()).isEqualTo(APPOINTMENT_ID);
            assertThat(notification.startTime()).isEqualTo(FUTURE_START);
            assertThat(notification.endTime()).isEqualTo(FUTURE_START.plusMinutes(30));
            assertThat(notification.clinicName()).isEqualTo("Clinica Demo");
            assertThat(notification.clinicSlug()).isEqualTo("demo");
            assertThat(notification.doctorName()).isEqualTo("Dr 10");
            assertThat(notification.specialtyName()).isEqualTo("Cardiologia");
            assertThat(notification.patientName()).isEqualTo("Patient 20");
            assertThat(notification.patientEmail()).isEqualTo("p20@mail.test");
            assertThat(notification.patientPhoneNumber()).isEqualTo("+59170000020");
            assertThat(notification.channels()).isEqualTo(AppointmentNotificationEvent.ALL_CHANNELS);
        }

        @Test
        void shouldRescheduleWhenDoctorIsAdministratorThatIsAlsoDoctor() {
            var adminDoctor = doctor(DOCTOR_ID, ERole.ADMINISTRATOR);
            adminDoctor.setAlsoDoctor(true);
            givenExistingForReschedule(adminDoctor, AppointmentStatus.PENDING);
            givenWorkingDay(DOCTOR_ID, FUTURE_DAY);
            givenSaveAssignsId();

            service.rescheduleAppointmentById(APPOINTMENT_ID, reschedule(FUTURE_START), DOCTOR_ID, false);

            assertThat(captureSaved().getStartTime()).isEqualTo(FUTURE_START);
        }

        private void givenLockedDoctorFor(Personal doctor) {
            when(appointmentRepository.findDoctorIdById(APPOINTMENT_ID)).thenReturn(Optional.of(doctor.getId()));
            when(personalRepository.findByIdForUpdate(doctor.getId())).thenReturn(Optional.of(doctor));
        }

        private void givenExistingForReschedule(Personal doctor, AppointmentStatus status) {
            givenLockedDoctorFor(doctor);
            var existing = appointment(APPOINTMENT_ID, doctor, patient(PATIENT_ID, true), CURRENT_START, status);
            when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(existing));
        }
    }

    @Nested
    @DisplayName("confirmAppointmentById")
    class ConfirmAppointment {
        @Test
        void shouldConfirmPendingAppointmentAndPublishConfirmedNotification() {
            givenExisting(AppointmentStatus.PENDING, FUTURE_START);
            givenSaveAssignsId();
            givenClinic();
            var response = givenMapperResponse();

            var result = service.confirmAppointmentById(APPOINTMENT_ID, 1L, false);

            assertThat(result).isSameAs(response);
            assertThat(captureSaved().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            var notification = singleNotification();
            assertThat(notification.type()).isEqualTo(AppointmentNotificationType.CONFIRMED);
            assertThat(notification.status()).isEqualTo(AppointmentStatus.CONFIRMED);
            assertThat(notification.channels()).isEqualTo(AppointmentNotificationEvent.ALL_CHANNELS);
            assertThat(publishedEvents())
                    .contains(new AppointmentChangedEvent(
                            CLINIC_ID, DOCTOR_ID, new BoardEvent(APPOINTMENT_ID, AppointmentStatus.CONFIRMED)));
        }

        @Test
        void shouldPublishNullSpecialtyWhenDoctorHasNoSpecialty() {
            var existing = givenExisting(AppointmentStatus.PENDING, FUTURE_START);
            existing.getDoctor().setSpecialty(null);
            givenSaveAssignsId();
            givenClinic();

            service.confirmAppointmentById(APPOINTMENT_ID, 1L, false);

            assertThat(singleNotification().specialtyName()).isNull();
        }

        @Test
        void shouldThrowConfirmOnlyPendingWhenAppointmentIsConfirmed() {
            givenExisting(AppointmentStatus.CONFIRMED, FUTURE_START);

            assertApiException(
                    () -> service.confirmAppointmentById(APPOINTMENT_ID, 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_CONFIRM_ONLY_PENDING);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowPastNotModifiableWhenConfirmingStartedAppointment() {
            givenExisting(AppointmentStatus.PENDING, NOW.minusMinutes(5));

            assertApiException(
                    () -> service.confirmAppointmentById(APPOINTMENT_ID, 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_PAST_NOT_MODIFIABLE);
        }

        @Test
        void shouldThrowForbiddenWhenRestrictedDoctorConfirmsAnotherDoctorsAppointment() {
            givenExisting(AppointmentStatus.PENDING, FUTURE_START);

            assertApiException(
                    () -> service.confirmAppointmentById(APPOINTMENT_ID, OTHER_DOCTOR_ID, true),
                    ForbiddenException.class,
                    MessageKeys.APPOINTMENT_UNAUTHORIZED);
        }

        @Test
        void shouldThrowNotFoundWhenConfirmingMissingAppointment() {
            when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.empty());

            assertApiException(
                    () -> service.confirmAppointmentById(APPOINTMENT_ID, 1L, false),
                    ResourceNotFoundException.class,
                    MessageKeys.APPOINTMENT_NOT_FOUND,
                    APPOINTMENT_ID);
        }
    }

    @Nested
    @DisplayName("cancelAppointmentById")
    class CancelAppointment {
        @Test
        void shouldCancelWithReasonAndPublishCancelledNotification() {
            givenExisting(AppointmentStatus.CONFIRMED, FUTURE_START);
            givenSaveAssignsId();
            givenClinic();

            service.cancelAppointmentById(APPOINTMENT_ID, new CancelAppointmentRequest("Viaje"), 1L, false);

            var saved = captureSaved();
            assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(saved.getCancellationReason()).isEqualTo("Viaje");
            var notification = singleNotification();
            assertThat(notification.type()).isEqualTo(AppointmentNotificationType.CANCELLED);
            assertThat(notification.status()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(notification.cancellationReason()).isEqualTo("Viaje");
            assertThat(notification.channels()).isEqualTo(AppointmentNotificationEvent.ALL_CHANNELS);
        }

        @Test
        void shouldThrowAlreadyCancelledWhenAppointmentIsCancelled() {
            givenExisting(AppointmentStatus.CANCELLED, FUTURE_START);

            assertApiException(
                    () -> service.cancelAppointmentById(APPOINTMENT_ID, new CancelAppointmentRequest("x"), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_ALREADY_CANCELLED);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldThrowPastNotModifiableWhenCancellingStartedAppointment() {
            givenExisting(AppointmentStatus.PENDING, NOW.minusMinutes(1));

            assertApiException(
                    () -> service.cancelAppointmentById(APPOINTMENT_ID, new CancelAppointmentRequest("x"), 1L, false),
                    BusinessException.class,
                    MessageKeys.APPOINTMENT_PAST_NOT_MODIFIABLE);
        }

        @Test
        void shouldAllowCancellingAppointmentStartingExactlyNow() {
            givenExisting(AppointmentStatus.PENDING, NOW);
            givenSaveAssignsId();
            givenClinic();

            service.cancelAppointmentById(APPOINTMENT_ID, new CancelAppointmentRequest("x"), 1L, false);

            assertThat(captureSaved().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test
        void shouldThrowForbiddenWhenRestrictedDoctorCancelsAnotherDoctorsAppointment() {
            givenExisting(AppointmentStatus.PENDING, FUTURE_START);

            assertApiException(
                    () -> service.cancelAppointmentById(
                            APPOINTMENT_ID, new CancelAppointmentRequest("x"), OTHER_DOCTOR_ID, true),
                    ForbiddenException.class,
                    MessageKeys.APPOINTMENT_UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("bulk cancellations")
    class BulkCancellations {
        @Test
        void shouldCancelAppointmentsInRangeAndNotifyOnAllChannels() {
            var from = FUTURE_START;
            var to = FUTURE_START.plusHours(2);
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            var first = appointment(1L, doctor, patient(PATIENT_ID, true), from, AppointmentStatus.PENDING);
            var second = appointment(
                    2L, doctor, patient(PATIENT_ID, true), from.plusMinutes(30), AppointmentStatus.CONFIRMED);
            when(appointmentRepository.findOverlappingAppointments(
                            DOCTOR_ID, from, to, List.of(AppointmentStatus.CANCELLED)))
                    .thenReturn(List.of(first, second));
            givenSaveAssignsId();
            givenClinic();

            var cancelled = service.cancelDoctorAppointmentsInRange(DOCTOR_ID, from, to, "Bloqueo");

            assertThat(cancelled).isEqualTo(2);
            assertThat(first.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(second.getCancellationReason()).isEqualTo("Bloqueo");
            assertThat(notifications()).hasSize(2).allSatisfy(n -> {
                assertThat(n.type()).isEqualTo(AppointmentNotificationType.CANCELLED);
                assertThat(n.channels()).isEqualTo(AppointmentNotificationEvent.ALL_CHANNELS);
            });
        }

        @Test
        void shouldReturnZeroWithoutLoadingClinicWhenNothingToCancel() {
            var cancelled = service.cancelDoctorAppointmentsInRange(
                    DOCTOR_ID, FUTURE_START, FUTURE_START.plusHours(1), "Bloqueo");

            assertThat(cancelled).isZero();
            verifyNoInteractions(currentClinicProvider);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        void shouldCancelUpcomingAppointmentsOfDeactivatedDoctorViaWhatsappOnly() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            var upcoming = appointment(1L, doctor, patient(PATIENT_ID, true), FUTURE_START, AppointmentStatus.PENDING);
            when(appointmentRepository.findByDoctorIdAndStatusInAndStartTimeAfterOrderByIdAsc(
                            DOCTOR_ID, List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED), NOW))
                    .thenReturn(List.of(upcoming));
            when(messageSource.getMessage(
                            eq(MessageKeys.APPOINTMENT_DOCTOR_DEACTIVATED_CANCELLATION_REASON),
                            isNull(),
                            any(Locale.class)))
                    .thenReturn("Doctor no disponible");
            givenSaveAssignsId();
            givenClinic();

            var cancelled = service.cancelUpcomingAppointmentsOfDeactivatedDoctor(DOCTOR_ID);

            assertThat(cancelled).isEqualTo(1);
            assertThat(upcoming.getCancellationReason()).isEqualTo("Doctor no disponible");
            var notification = singleNotification();
            assertThat(notification.type()).isEqualTo(AppointmentNotificationType.CANCELLED);
            assertThat(notification.channels()).isEqualTo(AppointmentNotificationEvent.WHATSAPP_ONLY);
        }

        @Test
        void shouldCancelUpcomingAppointmentsOfDeactivatedPatientViaWhatsappOnly() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            var upcoming =
                    appointment(1L, doctor, patient(PATIENT_ID, true), FUTURE_START, AppointmentStatus.CONFIRMED);
            when(appointmentRepository.findByPatientIdAndStatusInAndStartTimeAfterOrderByIdAsc(
                            PATIENT_ID, List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED), NOW))
                    .thenReturn(List.of(upcoming));
            when(messageSource.getMessage(
                            eq(MessageKeys.APPOINTMENT_PATIENT_DEACTIVATED_CANCELLATION_REASON),
                            isNull(),
                            any(Locale.class)))
                    .thenReturn("Paciente desactivado");
            givenSaveAssignsId();
            givenClinic();

            var cancelled = service.cancelUpcomingAppointmentsOfDeactivatedPatient(PATIENT_ID);

            assertThat(cancelled).isEqualTo(1);
            assertThat(upcoming.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(upcoming.getCancellationReason()).isEqualTo("Paciente desactivado");
            assertThat(singleNotification().channels()).isEqualTo(AppointmentNotificationEvent.WHATSAPP_ONLY);
        }
    }

    @Nested
    @DisplayName("queries")
    class Queries {
        @Test
        void shouldReturnMappedAppointmentWhenFindingById() {
            givenExisting(AppointmentStatus.PENDING, FUTURE_START);
            var response = givenMapperResponse();

            assertThat(service.findAppointmentById(APPOINTMENT_ID, DOCTOR_ID, true))
                    .isSameAs(response);
        }

        @Test
        void shouldThrowForbiddenWhenRestrictedDoctorReadsAnotherDoctorsAppointment() {
            givenExisting(AppointmentStatus.PENDING, FUTURE_START);

            assertApiException(
                    () -> service.findAppointmentById(APPOINTMENT_ID, OTHER_DOCTOR_ID, true),
                    ForbiddenException.class,
                    MessageKeys.APPOINTMENT_UNAUTHORIZED);
        }

        @Test
        void shouldPassNullFiltersThroughWhenListingAppointments() {
            var pageable = PageRequest.of(0, 20);
            var appointment = appointment(
                    APPOINTMENT_ID,
                    doctor(DOCTOR_ID, ERole.DOCTOR),
                    patient(PATIENT_ID, true),
                    FUTURE_START,
                    AppointmentStatus.PENDING);
            when(appointmentRepository.findAllByFilters(null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(appointment), pageable, 1));
            var response = givenMapperResponse();

            Page<AppointmentResponse> page = service.findAllAppointments(null, null, null, pageable);

            assertThat(page.getContent()).containsExactly(response);
            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        void shouldGroupBoardByStatusIncludingEmptyStatusesAndInclusiveEndDate() {
            var from = LocalDate.of(2026, 11, 9);
            var to = LocalDate.of(2026, 11, 10);
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            var pending = appointment(1L, doctor, patient(PATIENT_ID, true), FUTURE_START, AppointmentStatus.PENDING);
            when(appointmentRepository.findByFiltersAndDateRange(
                            DOCTOR_ID,
                            null,
                            from.atStartOfDay(),
                            LocalDate.of(2026, 11, 11).atStartOfDay()))
                    .thenReturn(List.of(pending));

            var board = service.getBoardByRange(from, to, DOCTOR_ID, null);

            assertThat(board).containsOnlyKeys(AppointmentStatus.values());
            assertThat(board.get(AppointmentStatus.CONFIRMED)).isEmpty();
            assertThat(board.get(AppointmentStatus.CANCELLED)).isEmpty();
            assertThat(board.get(AppointmentStatus.PENDING))
                    .containsExactly(new AppointmentSummaryItem(
                            1L,
                            "Patient 20",
                            "Dr 10",
                            FUTURE_START.toLocalDate(),
                            FUTURE_START.toLocalTime(),
                            FUTURE_START,
                            FUTURE_START.plusMinutes(30),
                            AppointmentStatus.PENDING,
                            DOCTOR_ID,
                            PATIENT_ID));
        }

        @Test
        void shouldQueryWholeMonthAndKeyCalendarByDay() {
            var doctor = doctor(DOCTOR_ID, ERole.DOCTOR);
            var first = appointment(1L, doctor, patient(PATIENT_ID, true), FUTURE_START, AppointmentStatus.PENDING);
            var second = appointment(
                    2L, doctor, patient(PATIENT_ID, true), FUTURE_START.plusHours(1), AppointmentStatus.CONFIRMED);
            var third = appointment(
                    3L, doctor, patient(PATIENT_ID, true), FUTURE_START.plusDays(1), AppointmentStatus.PENDING);
            when(appointmentRepository.findByFiltersAndDateRange(
                            null, PATIENT_ID, LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 12, 1, 0, 0)))
                    .thenReturn(List.of(first, second, third));

            var calendar = service.getCalendar(11, 2026, null, PATIENT_ID);

            assertThat(calendar).containsOnlyKeys("11-10-2026", "11-11-2026");
            assertThat(calendar.get("11-10-2026"))
                    .extracting(AppointmentSummaryItem::id)
                    .containsExactly(1L, 2L);
        }

        @Test
        void shouldQueryUntilNextYearWhenCalendarMonthIsDecember() {
            service.getCalendar(12, 2026, DOCTOR_ID, null);

            verify(appointmentRepository)
                    .findByFiltersAndDateRange(
                            DOCTOR_ID, null, LocalDateTime.of(2026, 12, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
        }
    }
}
