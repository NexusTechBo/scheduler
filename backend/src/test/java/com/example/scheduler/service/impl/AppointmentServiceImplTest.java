package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.DoctorAvailability;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.Role;
import com.example.scheduler.entity.ScheduleException;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.AppointmentMapper;
import com.example.scheduler.repository.*;
import com.example.scheduler.websocket.AppointmentChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    private static final long DOCTOR_ID = 7L;
    private static final LocalDateTime START = LocalDateTime.of(2099, 10, 12, 9, 0);
    private static final LocalDateTime END = START.plusMinutes(30);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"), ZoneId.of("America/La_Paz"));

    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientRepository patientRepository;
    @Mock PersonalRepository personalRepository;
    @Mock DoctorAvailabilityRepository doctorAvailabilityRepository;
    @Mock ScheduleExceptionRepository scheduleExceptionRepository;
    @Mock AppointmentMapper appointmentMapper;
    @Mock ApplicationEventPublisher eventPublisher;
    AppointmentServiceImpl service;

    private final AppointmentRequest request = new AppointmentRequest(DOCTOR_ID, 99L, START);

    @BeforeEach
    void freeSlot() {
        service = new AppointmentServiceImpl(appointmentRepository, patientRepository, personalRepository, appointmentMapper,
                scheduleExceptionRepository, doctorAvailabilityRepository, eventPublisher, CLOCK);
        TenantContext.setCurrentTenant("clinic_3");
        lenient().when(doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(DOCTOR_ID, START.getDayOfWeek()))
                .thenReturn(List.of(DoctorAvailability.builder()
                        .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(13, 0)).slotDurationMinutes(30).build()));
        lenient().when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(personal(ERole.DOCTOR, true)));
        lenient().when(patientRepository.findById(anyLong())).thenReturn(Optional.of(Patient.builder().active(true).build()));
    }

    @Test
    @DisplayName("B15: unknown doctorId -> 404 Doctor not found, nothing saved")
    void bookAppointment_should_throwNotFound_when_doctorUnknown() {
        when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bookAppointment(request, 5L, "PATIENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Doctor not found with id: " + DOCTOR_ID);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("B15: doctorId of an assistant -> 400 is not a doctor")
    void bookAppointment_should_throwBadRequest_when_personalIsNotDoctor() {
        when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(personal(ERole.ASSISTANT, true)));

        assertThatThrownBy(() -> service.bookAppointment(request, 5L, "PATIENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Personal with id " + DOCTOR_ID + " is not a doctor");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("B16: inactive doctor -> 409")
    void bookAppointment_should_throwConflict_when_doctorInactive() {
        when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(personal(ERole.DOCTOR, false)));

        assertThatThrownBy(() -> service.bookAppointment(request, 5L, "PATIENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The doctor is not active");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("B15: unknown patientId (staff booking) -> 404 Patient not found")
    void bookAppointment_should_throwNotFound_when_patientUnknown() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bookAppointment(request, 1L, "ASSISTANT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Patient not found with id: 99");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("B16: inactive patient -> 409")
    void bookAppointment_should_throwConflict_when_patientInactive() {
        when(patientRepository.findById(99L)).thenReturn(Optional.of(Patient.builder().active(false).build()));

        assertThatThrownBy(() -> service.bookAppointment(request, 1L, "ASSISTANT"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The patient is not active");
        verify(appointmentRepository, never()).save(any());
    }

    private static Personal personal(ERole role, boolean active) {
        return Personal.builder().id(DOCTOR_ID).active(active).role(Role.builder().name(role).build()).build();
    }

    @Test
    void bookAppointment_should_forceCallerAndPending_when_patient() {
        stubSave();

        service.bookAppointment(request, 5L, "PATIENT");

        verify(patientRepository).findById(5L);
        assertThat(savedAppointment().getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void bookAppointment_should_computeEndTimeFromSlotDuration() {
        stubSave();

        service.bookAppointment(request, 5L, "PATIENT");

        assertThat(savedAppointment().getEndTime()).isEqualTo(END);
    }

    @Test
    void bookAppointment_should_reject_when_startTimeOffSlotGrid() {
        var offGrid = new AppointmentRequest(DOCTOR_ID, 99L, START.plusMinutes(10));

        assertThatThrownBy(() -> service.bookAppointment(offGrid, 5L, "PATIENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The doctor does not attend at that time");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_should_reject_when_inThePast() {
        var past = new AppointmentRequest(DOCTOR_ID, 99L, LocalDateTime.now(CLOCK).minusDays(1));

        assertThatThrownBy(() -> service.bookAppointment(past, 5L, "PATIENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The appointment cannot be in the past");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_should_reject_when_slotStartsAtPartialBlockStart() {
        var at8 = new AppointmentRequest(DOCTOR_ID, 99L, START.withHour(8));
        when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, START.toLocalDate()))
                .thenReturn(List.of(partialBlock(8, 9)));

        assertThatThrownBy(() -> service.bookAppointment(at8, 5L, "PATIENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("That schedule is blocked");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_should_reject_when_fullDayBlock() {
        when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, START.toLocalDate()))
                .thenReturn(List.of(ScheduleException.builder().isFullDayBlock(true).build()));

        assertThatThrownBy(() -> service.bookAppointment(request, 5L, "PATIENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("That schedule is blocked");
    }

    @Test
    void bookAppointment_should_book_when_partialBlockEndsAtSlotStart() {
        when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, START.toLocalDate()))
                .thenReturn(List.of(partialBlock(8, 9)));
        stubSave();

        service.bookAppointment(request, 5L, "PATIENT");

        assertThat(savedAppointment().getStartTime()).isEqualTo(START);
    }

    @Test
    void bookAppointment_should_notFail500_when_legacyBlockHasZeroSlotDuration() {
        // B1 residue: createGeneratedSlots skips rows with duration <= 0, findSlotEnd still computes offset % 0.
        when(doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(DOCTOR_ID, START.getDayOfWeek()))
                .thenReturn(List.of(
                        // starts before the requested 09:00, so the `offset >= 0` guard does not short-circuit
                        DoctorAvailability.builder().startTime(LocalTime.of(7, 0)).endTime(LocalTime.of(8, 0)).slotDurationMinutes(0).build(),
                        DoctorAvailability.builder().startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(13, 0)).slotDurationMinutes(30).build()));
        stubSave();

        service.bookAppointment(request, 5L, "PATIENT");

        assertThat(savedAppointment().getEndTime()).isEqualTo(END);
    }

    private static ScheduleException partialBlock(int fromHour, int toHour) {
        return ScheduleException.builder().isFullDayBlock(false)
                .startTime(LocalTime.of(fromHour, 0)).endTime(LocalTime.of(toHour, 0)).build();
    }

    @Test
    void bookAppointment_should_reject_when_partialBlockOverlapsSlot() {
        when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, START.toLocalDate()))
                .thenReturn(List.of(ScheduleException.builder().isFullDayBlock(false)
                        .startTime(LocalTime.of(8, 45)).endTime(LocalTime.of(9, 15)).build()));

        assertThatThrownBy(() -> service.bookAppointment(request, 5L, "PATIENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("That schedule is blocked");
        verify(appointmentRepository, never()).save(any());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void bookAppointment_should_publishBoardEventForCallerClinic() {
        stubSave();

        service.bookAppointment(request, 5L, "PATIENT");

        var captor = ArgumentCaptor.forClass(AppointmentChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().clinicId()).isEqualTo(3L);
        assertThat(captor.getValue().payload().status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void bookAppointment_should_useBodyPatientAndPending_when_staff() {
        stubSave();

        service.bookAppointment(request, 1L, "ASSISTANT");

        verify(patientRepository).findById(99L);
        assertThat(savedAppointment().getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void bookAppointment_should_throwConflict_when_slotOverlapsActiveAppointment() {
        when(appointmentRepository.existsOverlappingAppointment(DOCTOR_ID, START, END, List.of(AppointmentStatus.CANCELLED)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.bookAppointment(request, 5L, "PATIENT"))
                .isInstanceOf(BusinessException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancelAppointment_should_storeStrippedReason() {
        stubExisting(AppointmentStatus.CONFIRMED);

        service.cancelAppointmentById(1L, new CancelAppointmentRequest("  El doctor no estará disponible "), 1L, "ASSISTANT");

        var saved = savedAppointment();
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(saved.getCancellationReason()).isEqualTo("El doctor no estará disponible");
    }

    @Test
    void cancelAppointment_should_storeNullReason_when_blankOrMissing() {
        stubExisting(AppointmentStatus.PENDING);

        service.cancelAppointmentById(1L, new CancelAppointmentRequest("   "), 1L, "ASSISTANT");

        assertThat(savedAppointment().getCancellationReason()).isNull();
    }

    private void stubExisting(AppointmentStatus status) {
        var appointment = Appointment.builder().id(1L).status(status).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubSave() {
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Appointment savedAppointment() {
        var captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        return captor.getValue();
    }
}
