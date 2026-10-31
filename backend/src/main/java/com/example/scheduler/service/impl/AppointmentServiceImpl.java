package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.AppointmentMapper;
import com.example.scheduler.notification.AppointmentNotificationEvent;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.websocket.AppointmentChangedEvent;
import com.example.scheduler.websocket.BoardEvent;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {
    private static final DateTimeFormatter CALENDAR_KEY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String CHANNEL_STAFF = "staff";
    private static final String CHANNEL_GUEST = "guest";

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PersonalRepository personalRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final ClinicRepository clinicRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        return book(request, CHANNEL_STAFF);
    }

    @Override
    @Transactional
    public AppointmentResponse bookGuestAppointment(AppointmentRequest request) {
        return book(request, CHANNEL_GUEST);
    }

    private AppointmentResponse book(AppointmentRequest request, String channel) {
        var patientId = request.patientId();
        var doctor = personalRepository
                .findByIdForUpdate(request.doctorId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, request.doctorId()));
        var endTime = verifySlotBookable(doctor, request.startTime());
        var patient = patientRepository
                .findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PATIENT_NOT_FOUND, patientId));
        if (!patient.isActive()) throw new BusinessException(MessageKeys.AUTH_USER_NOT_ACTIVE);
        var appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .startTime(request.startTime())
                .endTime(endTime)
                .status(AppointmentStatus.PENDING)
                .build();
        var saved = saveAndNotify(appointment);
        log.atInfo()
                .addKeyValue("channel", channel)
                .log(
                        "Appointment {} created via {} for doctor {} and patient {} with status {}",
                        saved.getId(),
                        channel,
                        doctor.getId(),
                        patientId,
                        saved.getStatus());
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LocalDateTime verifySlotBookable(Personal doctor, LocalDateTime startTime) {
        var endTime = findSlotEnd(doctor.getId(), startTime)
                .orElseThrow(() -> new BusinessException(MessageKeys.APPOINTMENT_OUT_OF_JOURNEY));
        if (doctor.getRole() != ERole.DOCTOR)
            throw new BadRequestException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, doctor.getId());
        if (!doctor.isActive()) throw new BusinessException(MessageKeys.AUTH_USER_NOT_ACTIVE);
        if (startTime.isBefore(LocalDateTime.now(clock)))
            throw new BadRequestException(MessageKeys.APPOINTMENT_PAST_DATE);
        if (isBlocked(doctor.getId(), startTime, endTime))
            throw new BusinessException(MessageKeys.APPOINTMENT_SCHEDULE_BLOCKED);
        if (isSlotTaken(doctor.getId(), startTime, endTime))
            throw new BusinessException(MessageKeys.APPOINTMENT_SLOT_TAKEN);
        return endTime;
    }

    @Override
    public AppointmentResponse findAppointmentById(Long appointmentId, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(appointmentId);
        verifyPermission(appointment, userId, role);
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
    public AppointmentResponse confirmAppointmentById(Long AppointmentId, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(AppointmentId);
        verifyPermission(appointment, userId, role);
        if (appointment.getStatus() != AppointmentStatus.PENDING)
            throw new BusinessException(MessageKeys.APPOINTMENT_CONFIRM_ONLY_PENDING);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        var saved = saveAndNotify(appointment);
        logStatusChange(saved, AppointmentStatus.PENDING);
        publishEmailNotification(saved, getCurrentClinicName());
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointmentById(
            Long AppointmentId, CancelAppointmentRequest request, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(AppointmentId);
        verifyPermission(appointment, userId, role);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED)
            throw new BusinessException(MessageKeys.APPOINTMENT_ALREADY_CANCELLED);
        var previousStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason());
        var saved = saveAndNotify(appointment);
        logStatusChange(saved, previousStatus);
        publishEmailNotification(saved, getCurrentClinicName());
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelAppointments(List<Appointment> appointments, String reason) {
        if (appointments.isEmpty()) return;
        var clinicName = getCurrentClinicName();
        for (Appointment appointment : appointments) {
            var previousStatus = appointment.getStatus();
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancellationReason(reason);
            var saved = saveAndNotify(appointment);
            logStatusChange(saved, previousStatus);
            publishEmailNotification(saved, clinicName);
        }
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
                TenantContext.getCurrentClinicId(), new BoardEvent(saved.getId(), saved.getStatus())));
        return saved;
    }

    private void publishEmailNotification(Appointment appointment, String clinicName) {
        var doctor = appointment.getDoctor();
        var patient = appointment.getPatient();
        if (!StringUtils.hasText(patient.getEmail())) {
            log.info(
                    "Appointment {} email notification skipped: patient {} has no email",
                    appointment.getId(),
                    patient.getId());
            return;
        }
        var specialty = doctor.getSpecialty();
        eventPublisher.publishEvent(new AppointmentNotificationEvent(
                appointment.getStatus(),
                appointment.getId(),
                clinicName,
                doctor.getName(),
                specialty != null ? specialty.getName() : null,
                patient.getName(),
                patient.getEmail(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getCancellationReason()));
    }

    private String getCurrentClinicName() {
        return clinicRepository
                .findById(TenantContext.getCurrentClinicId())
                .map(Clinic::getName)
                .orElse(null);
    }

    private Appointment getAppointmentOrThrowById(Long appointmentId) {
        return appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.APPOINTMENT_NOT_FOUND, appointmentId));
    }

    private void verifyPermission(Appointment appointment, Long userId, String role) {
        if (ERole.DOCTOR.name().equals(role))
            if (!appointment.getDoctor().getId().equals(userId))
                throw new ForbiddenException(MessageKeys.APPOINTMENT_UNAUTHORIZED);
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

    private boolean isSlotTaken(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        return appointmentRepository.existsOverlappingAppointment(
                doctorId, startTime, endTime, List.of(AppointmentStatus.CANCELLED));
    }
}
