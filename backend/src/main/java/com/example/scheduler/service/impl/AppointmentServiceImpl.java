package com.example.scheduler.service.impl;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.dto.appointment.RescheduleAppointmentRequest;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.AppointmentMapper;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.AppointmentNotificationType;
import com.example.scheduler.notification.event.NotificationChannel;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.tenant.TenantContext;
import com.example.scheduler.util.DateTimeFormats;
import com.example.scheduler.websocket.event.AppointmentChangedEvent;
import com.example.scheduler.websocket.event.BoardEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {
    private static final DateTimeFormatter CALENDAR_KEY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final List<AppointmentStatus> EXCLUDED_FROM_OVERLAP = List.of(AppointmentStatus.CANCELLED);
    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PersonalRepository personalRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final CurrentClinicProvider currentClinicProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;
    private final Clock clock;

    /**
     * Who is booking. Staff see the existing appointment's id and status in the conflict; guests only see its start
     * time, so a public caller cannot learn more about the appointment.
     */
    private enum BookingChannel {
        STAFF("staff", MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE, true),
        GUEST("guest", MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE_GUEST, false);

        private final String label;
        private final String conflictMessageKey;
        private final boolean exposesAppointmentState;

        BookingChannel(String label, String conflictMessageKey, boolean exposesAppointmentState) {
            this.label = label;
            this.conflictMessageKey = conflictMessageKey;
            this.exposesAppointmentState = exposesAppointmentState;
        }
    }

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, Long userId, boolean restrictToOwn) {
        if (restrictToOwn && !request.doctorId().equals(userId))
            throw new ForbiddenException(MessageKeys.APPOINTMENT_UNAUTHORIZED);
        return book(request, BookingChannel.STAFF);
    }

    @Override
    @Transactional
    public AppointmentResponse bookGuestAppointment(AppointmentRequest request) {
        return book(request, BookingChannel.GUEST);
    }

    private AppointmentResponse book(AppointmentRequest request, BookingChannel channel) {
        var patientId = request.patientId();
        var doctor = personalRepository
                .findByIdForUpdate(request.doctorId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, request.doctorId()));
        var endTime = verifySlotBookable(doctor, request.startTime(), null);
        var patient = patientRepository
                .findByIdForShare(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PATIENT_NOT_FOUND, patientId));
        if (!patient.isActive()) throw new BusinessException(MessageKeys.APPOINTMENT_PATIENT_INACTIVE);
        verifyNoActiveAppointmentWithDoctor(patientId, doctor.getId(), channel);
        var appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .startTime(request.startTime())
                .endTime(endTime)
                .status(AppointmentStatus.PENDING)
                .createdAt(LocalDateTime.now(clock))
                .build();
        var saved = saveAndNotify(appointment);
        log.atInfo()
                .addKeyValue("channel", channel.label)
                .log(
                        "Appointment {} created via {} for doctor {} and patient {} with status {}",
                        saved.getId(),
                        channel.label,
                        doctor.getId(),
                        patientId,
                        saved.getStatus());
        return appointmentMapper.toResponse(saved);
    }

    /**
     * Rejects a booking when the patient already has an upcoming active appointment with the same doctor. The caller
     * holds the pessimistic lock on the doctor ({@code findByIdForUpdate}), which serializes every booking for that
     * doctor; since the rule is per doctor, no extra lock or unique index is needed.
     */
    private void verifyNoActiveAppointmentWithDoctor(Long patientId, Long doctorId, BookingChannel channel) {
        appointmentRepository
                .findFirstByPatientIdAndDoctorIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
                        patientId, doctorId, ACTIVE_STATUSES, LocalDateTime.now(clock))
                .ifPresent(existing -> {
                    throw patientHasActiveConflict(existing, channel);
                });
    }

    private static BusinessException patientHasActiveConflict(Appointment existing, BookingChannel channel) {
        var startTime = existing.getStartTime();
        var date = startTime.format(DateTimeFormats.SHORT_DATE);
        var time = startTime.format(DateTimeFormats.TIME);
        var details = new LinkedHashMap<String, Object>();
        details.put("startTime", startTime);
        if (!channel.exposesAppointmentState)
            return BusinessException.withDetails(details, channel.conflictMessageKey, date, time);
        details.put("appointmentId", existing.getId());
        details.put("status", existing.getStatus());
        return BusinessException.withDetails(
                details, channel.conflictMessageKey, date, time, statusChoice(existing.getStatus()));
    }

    /** Selector for the {@code choice} format of the status label in the message bundles (0 pending, 1 confirmed). */
    private static int statusChoice(AppointmentStatus status) {
        return status == AppointmentStatus.CONFIRMED ? 1 : 0;
    }

    private LocalDateTime verifySlotBookable(Personal doctor, LocalDateTime startTime, Long excludeAppointmentId) {
        if (!doctor.isDoctor()) throw new BadRequestException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, doctor.getId());
        if (!doctor.isActive()) throw new BusinessException(MessageKeys.APPOINTMENT_DOCTOR_INACTIVE);
        if (startTime.isBefore(LocalDateTime.now(clock)))
            throw new BadRequestException(MessageKeys.APPOINTMENT_PAST_DATE);
        var endTime = findSlotEnd(doctor.getId(), startTime)
                .orElseThrow(() -> new BusinessException(MessageKeys.APPOINTMENT_OUT_OF_JOURNEY));
        if (isBlocked(doctor.getId(), startTime, endTime))
            throw new BusinessException(MessageKeys.APPOINTMENT_SCHEDULE_BLOCKED);
        if (isSlotTaken(doctor.getId(), startTime, endTime, excludeAppointmentId))
            throw new BusinessException(MessageKeys.APPOINTMENT_SLOT_TAKEN);
        return endTime;
    }

    @Override
    public AppointmentResponse findAppointmentById(Long appointmentId, Long userId, boolean restrictToOwn) {
        var appointment = getAppointmentOrThrowById(appointmentId);
        verifyPermission(appointment, userId, restrictToOwn);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    public Page<AppointmentResponse> findAllAppointments(
            Long doctorId, Long patientId, AppointmentStatus status, Pageable pageable) {
        return appointmentRepository
                .findAllByFilters(doctorId, patientId, status, pageable)
                .map(appointmentMapper::toResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse confirmAppointmentById(Long appointmentId, Long userId, boolean restrictToOwn) {
        var appointment = getAppointmentOrThrowById(appointmentId);
        verifyPermission(appointment, userId, restrictToOwn);
        if (appointment.getStatus() != AppointmentStatus.PENDING)
            throw new BusinessException(MessageKeys.APPOINTMENT_CONFIRM_ONLY_PENDING);
        verifyNotStarted(appointment);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        var saved = saveAndNotify(appointment);
        logStatusChange(saved, AppointmentStatus.PENDING);
        publishPatientNotification(
                saved,
                currentClinicProvider.getCurrentClinic(),
                AppointmentNotificationType.CONFIRMED,
                AppointmentNotificationEvent.ALL_CHANNELS);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointmentById(
            Long appointmentId, RescheduleAppointmentRequest request, Long userId, boolean restrictToOwn) {
        // Lock the doctor before reading anything else, as book() does: bookings and reschedules of the same doctor are
        // serialized, so the doctor checks and the slot checks below see committed, locked state. The appointment row
        // itself is protected by its @Version column against a concurrent confirm or cancel.
        var doctorId = appointmentRepository
                .findDoctorIdById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.APPOINTMENT_NOT_FOUND, appointmentId));
        var doctor = personalRepository
                .findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, doctorId));
        var appointment = getAppointmentOrThrowById(appointmentId);
        verifyPermission(appointment, userId, restrictToOwn);
        var previousStatus = appointment.getStatus();
        if (!ACTIVE_STATUSES.contains(previousStatus))
            throw new BusinessException(MessageKeys.APPOINTMENT_RESCHEDULE_ONLY_ACTIVE);
        verifyNotStarted(appointment);
        var previousStart = appointment.getStartTime();
        var newStart = request.startTime();
        if (newStart.equals(previousStart)) throw new BusinessException(MessageKeys.APPOINTMENT_RESCHEDULE_SAME_TIME);
        var newEnd = verifySlotBookable(doctor, newStart, appointment.getId());
        appointment.setStartTime(newStart);
        appointment.setEndTime(newEnd);
        appointment.setStatus(AppointmentStatus.PENDING);
        var saved = saveAndNotify(appointment);
        log.info(
                "Appointment {} of doctor {} rescheduled {} -> {} (status {} -> {})",
                saved.getId(),
                doctorId,
                previousStart,
                newStart,
                previousStatus,
                saved.getStatus());
        if (previousStatus == AppointmentStatus.CONFIRMED)
            publishPatientNotification(
                    saved,
                    currentClinicProvider.getCurrentClinic(),
                    AppointmentNotificationType.RESCHEDULED,
                    AppointmentNotificationEvent.ALL_CHANNELS);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointmentById(
            Long appointmentId, CancelAppointmentRequest request, Long userId, boolean restrictToOwn) {
        var appointment = getAppointmentOrThrowById(appointmentId);
        verifyPermission(appointment, userId, restrictToOwn);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED)
            throw new BusinessException(MessageKeys.APPOINTMENT_ALREADY_CANCELLED);
        verifyNotStarted(appointment);
        var saved = cancel(appointment, request.reason());
        publishPatientNotification(
                saved,
                currentClinicProvider.getCurrentClinic(),
                AppointmentNotificationType.CANCELLED,
                AppointmentNotificationEvent.ALL_CHANNELS);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public int cancelDoctorAppointmentsInRange(Long doctorId, LocalDateTime from, LocalDateTime to, String reason) {
        var appointments = appointmentRepository.findOverlappingAppointments(doctorId, from, to, EXCLUDED_FROM_OVERLAP);
        return cancelAll(appointments, reason, AppointmentNotificationEvent.ALL_CHANNELS);
    }

    @Override
    @Transactional
    public int cancelUpcomingAppointmentsOfDeactivatedDoctor(Long doctorId) {
        var appointments = appointmentRepository.findByDoctorIdAndStatusInAndStartTimeAfterOrderByIdAsc(
                doctorId, ACTIVE_STATUSES, LocalDateTime.now(clock));
        var cancelled = cancelAll(
                appointments,
                resolveMessage(MessageKeys.APPOINTMENT_DOCTOR_DEACTIVATED_CANCELLATION_REASON),
                AppointmentNotificationEvent.WHATSAPP_ONLY);
        if (cancelled > 0) log.info("{} upcoming appointments of deactivated doctor {} cancelled", cancelled, doctorId);
        return cancelled;
    }

    @Override
    @Transactional
    public int cancelUpcomingAppointmentsOfDeactivatedPatient(Long patientId) {
        var appointments = appointmentRepository.findByPatientIdAndStatusInAndStartTimeAfterOrderByIdAsc(
                patientId, ACTIVE_STATUSES, LocalDateTime.now(clock));
        var cancelled = cancelAll(
                appointments,
                resolveMessage(MessageKeys.APPOINTMENT_PATIENT_DEACTIVATED_CANCELLATION_REASON),
                AppointmentNotificationEvent.WHATSAPP_ONLY);
        if (cancelled > 0)
            log.info("{} upcoming appointments of deactivated patient {} cancelled", cancelled, patientId);
        return cancelled;
    }

    private int cancelAll(List<Appointment> appointments, String reason, Set<NotificationChannel> channels) {
        if (appointments.isEmpty()) return 0;
        var clinic = currentClinicProvider.getCurrentClinic();
        for (Appointment appointment : appointments) {
            var saved = cancel(appointment, reason);
            publishPatientNotification(saved, clinic, AppointmentNotificationType.CANCELLED, channels);
        }
        return appointments.size();
    }

    private Appointment cancel(Appointment appointment, String reason) {
        var previousStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        var saved = saveAndNotify(appointment);
        logStatusChange(saved, previousStatus);
        return saved;
    }

    private static void logStatusChange(Appointment appointment, AppointmentStatus from) {
        log.info(
                "Appointment {} of doctor {} changed {} -> {}",
                appointment.getId(),
                appointment.getDoctor().getId(),
                from,
                appointment.getStatus());
    }

    @Override
    public Map<AppointmentStatus, List<AppointmentSummaryItem>> getBoardByRange(
            LocalDate from, LocalDate to, Long doctorId, Long patientId) {
        return groupByStatus(appointmentRepository.findByFiltersAndDateRange(
                doctorId, patientId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }

    @Override
    public Map<String, List<AppointmentSummaryItem>> getCalendar(int month, int year, Long doctorId, Long patientId) {
        var monthStart = LocalDate.of(year, month, 1);
        return groupByDay(appointmentRepository.findByFiltersAndDateRange(
                doctorId,
                patientId,
                monthStart.atStartOfDay(),
                monthStart.plusMonths(1).atStartOfDay()));
    }

    private Map<AppointmentStatus, List<AppointmentSummaryItem>> groupByStatus(List<Appointment> appointments) {
        var board = new EnumMap<AppointmentStatus, List<AppointmentSummaryItem>>(AppointmentStatus.class);
        for (AppointmentStatus status : AppointmentStatus.values()) board.put(status, new ArrayList<>());
        for (Appointment appointment : appointments)
            board.get(appointment.getStatus()).add(toSummaryItem(appointment));
        return board;
    }

    private Map<String, List<AppointmentSummaryItem>> groupByDay(List<Appointment> appointments) {
        var calendar = new LinkedHashMap<String, List<AppointmentSummaryItem>>();
        for (Appointment appointment : appointments) {
            String key = appointment.getStartTime().toLocalDate().format(CALENDAR_KEY_FORMATTER);
            calendar.computeIfAbsent(key, _ -> new ArrayList<>()).add(toSummaryItem(appointment));
        }
        return calendar;
    }

    private AppointmentSummaryItem toSummaryItem(Appointment appointment) {
        var startTime = appointment.getStartTime();
        return new AppointmentSummaryItem(
                appointment.getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName(),
                startTime.toLocalDate(),
                startTime.toLocalTime(),
                startTime,
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getDoctor().getId(),
                appointment.getPatient().getId());
    }

    private Appointment saveAndNotify(Appointment appointment) {
        var saved = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(new AppointmentChangedEvent(
                TenantContext.getCurrentClinicId(),
                saved.getDoctor().getId(),
                new BoardEvent(saved.getId(), saved.getStatus())));
        return saved;
    }

    private void publishPatientNotification(
            Appointment appointment,
            Clinic clinic,
            AppointmentNotificationType type,
            Set<NotificationChannel> channels) {
        var doctor = appointment.getDoctor();
        var patient = appointment.getPatient();
        var specialty = doctor.getSpecialty();
        eventPublisher.publishEvent(new AppointmentNotificationEvent(
                type,
                appointment.getStatus(),
                appointment.getId(),
                clinic.getName(),
                clinic.getSlug(),
                doctor.getName(),
                specialty != null ? specialty.getName() : null,
                patient.getName(),
                patient.getEmail(),
                patient.getPhoneNumber(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getCancellationReason(),
                channels));
    }

    private String resolveMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    private Appointment getAppointmentOrThrowById(Long appointmentId) {
        return appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.APPOINTMENT_NOT_FOUND, appointmentId));
    }

    private void verifyPermission(Appointment appointment, Long userId, boolean restrictToOwn) {
        if (restrictToOwn && !appointment.getDoctor().getId().equals(userId))
            throw new ForbiddenException(MessageKeys.APPOINTMENT_UNAUTHORIZED);
    }

    private void verifyNotStarted(Appointment appointment) {
        if (appointment.getStartTime().isBefore(LocalDateTime.now(clock)))
            throw new BusinessException(MessageKeys.APPOINTMENT_PAST_NOT_MODIFIABLE);
    }

    private boolean isBlocked(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        return scheduleExceptionRepository.findByDoctorIdAndDate(doctorId, startTime.toLocalDate()).stream()
                .anyMatch(e -> e.blocks(startTime.toLocalTime(), endTime.toLocalTime()));
    }

    private Optional<LocalDateTime> findSlotEnd(Long doctorId, LocalDateTime startTime) {
        var time = startTime.toLocalTime();
        return doctorAvailabilityRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, startTime.getDayOfWeek())
                .stream()
                .filter(a -> a.getSlotDurationMinutes() != null && a.getSlotDurationMinutes() > 0)
                .filter(a -> {
                    var slotSeconds = a.getSlotDurationMinutes() * 60L;
                    var offset = Duration.between(a.getStartTime(), time).toSeconds();
                    return offset >= 0
                            && offset % slotSeconds == 0
                            && Duration.between(time, a.getEndTime()).toSeconds() >= slotSeconds;
                })
                .findFirst()
                .map(a -> startTime.plusMinutes(a.getSlotDurationMinutes()));
    }

    private boolean isSlotTaken(
            Long doctorId, LocalDateTime startTime, LocalDateTime endTime, Long excludeAppointmentId) {
        return appointmentRepository.existsOverlappingAppointment(
                doctorId, startTime, endTime, EXCLUDED_FROM_OVERLAP, excludeAppointmentId);
    }
}
