package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.AppointmentMapper;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.websocket.AppointmentChangedEvent;
import com.example.scheduler.websocket.BoardEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {
    private static final DateTimeFormatter CALENDAR_KEY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PersonalRepository personalRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleExceptionRepository  scheduleExceptionRepository;
    private final DoctorAvailabilityRepository  doctorAvailabilityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, Long userId, String role) {
        var isPatient = ERole.PATIENT.name().equals(role);
        var patientId = isPatient ? userId : request.patientId();
        var startTime = request.startTime();
        if (startTime.isBefore(LocalDateTime.now(clock)))
            throw new BadRequestException("The appointment cannot be in the past");
        var doctor = personalRepository.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + request.doctorId()));
        if (doctor.getRole().getName() != ERole.DOCTOR)
            throw new BadRequestException("Personal with id " + request.doctorId() + " is not a doctor");
        if (!doctor.isActive())
            throw new BusinessException("The doctor is not active");
        var patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
        if (!patient.isActive())
            throw new BusinessException("The patient is not active");
        var endTime = findSlotEnd(request.doctorId(), startTime)
                .orElseThrow(() -> new BusinessException("The doctor does not attend at that time"));
        if (isBlocked(request.doctorId(), startTime, endTime))
            throw new BusinessException("That schedule is blocked");
        if (isSlotTaken(request.doctorId(), startTime, endTime))
            throw new BusinessException("That slot is already taken");
        var appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .startTime(startTime)
                .endTime(endTime)
                .status(AppointmentStatus.PENDING)
                .build();
        return appointmentMapper.toResponse(saveAndNotify(appointment));
    }

    @Override
    public AppointmentResponse findAppointmentById(Long appointmentId, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(appointmentId);
        verifyPermission(appointment, userId, role);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    public Page<AppointmentResponse> findAllAppointments(Long doctorId, Long patientId, AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findAllByFilters(doctorId, patientId, status, pageable).map(appointmentMapper::toResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse confirmAppointmentById(Long AppointmentId, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(AppointmentId);
        verifyPermission(appointment, userId, role);
        if (appointment.getStatus() != AppointmentStatus.PENDING)
            throw new BusinessException("Just can confirm an appointment pending");
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentMapper.toResponse(saveAndNotify(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointmentById(Long AppointmentId, CancelAppointmentRequest request, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(AppointmentId);
        verifyPermission(appointment, userId, role);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED)
            throw new BusinessException("This appointment is already cancelled");
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason().strip());
        return appointmentMapper.toResponse(saveAndNotify(appointment));
    }

    @Override
    @Transactional
    public void cancelAppointments(List<Appointment> appointments, String reason) {
        for (Appointment appointment : appointments) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancellationReason(reason);
            saveAndNotify(appointment);
        }
    }

    @Override
    public Map<AppointmentStatus, List<AppointmentSummaryItem>> getBoardByRange(LocalDate from, LocalDate to, Long doctorId, Long patientId) {
        return groupByStatus(appointmentRepository.findByFiltersAndDateRange(doctorId, patientId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }

    @Override
    public Map<String, List<AppointmentSummaryItem>> getCalendar(int month, int year, Long doctorId, Long patientId) {
        var monthStart = LocalDate.of(year, month, 1);
        return groupByDay(appointmentRepository.findByFiltersAndDateRange(doctorId, patientId, monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay()));
    }

    private Map<AppointmentStatus, List<AppointmentSummaryItem>> groupByStatus(List<Appointment> appointments) {
        var board = new EnumMap<AppointmentStatus, List<AppointmentSummaryItem>>(AppointmentStatus.class);
        for (AppointmentStatus status : AppointmentStatus.values())
            board.put(status, new ArrayList<>());
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
                appointment.getPatient().getAccount().getName(),
                appointment.getDoctor().getAccount().getName(),
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
        var clinicId = Long.valueOf(TenantContext.getCurrentTenant().substring("clinic_".length()));
        eventPublisher.publishEvent(new AppointmentChangedEvent(clinicId, new BoardEvent(saved.getId(), saved.getStatus())));
        return saved;
    }

    private Appointment getAppointmentOrThrowById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
    }

    private void verifyPermission(Appointment appointment, Long userId, String role) {
        if(role.equals(ERole.DOCTOR.name()))
            if (!appointment.getDoctor().getId().equals(userId))
                throw new ForbiddenException("Not authorize to do this");
        if(role.equals(ERole.PATIENT.name()))
            if (!appointment.getPatient().getId().equals(userId))
                throw new ForbiddenException("Not authorize to do this");
    }

    private boolean isBlocked(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        return scheduleExceptionRepository.findByDoctorIdAndDate(doctorId, startTime.toLocalDate()).stream()
                .anyMatch(e -> e.blocks(startTime.toLocalTime(), endTime.toLocalTime()));
    }

    private Optional<LocalDateTime> findSlotEnd(Long doctorId, LocalDateTime startTime) {
        var time = startTime.toLocalTime();
        return doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, startTime.getDayOfWeek()).stream()
                .filter(a -> a.getSlotDurationMinutes() != null && a.getSlotDurationMinutes() > 0)
                .filter(a -> {
                    var slotSeconds = a.getSlotDurationMinutes() * 60L;
                    var offset = Duration.between(a.getStartTime(), time).toSeconds();
                    return offset >= 0 && offset % slotSeconds == 0
                            && Duration.between(time, a.getEndTime()).toSeconds() >= slotSeconds;
                })
                .findFirst()
                .map(a -> startTime.plusMinutes(a.getSlotDurationMinutes()));
    }

    private boolean isSlotTaken(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        return appointmentRepository.existsOverlappingAppointment(
                doctorId,
                startTime,
                endTime,
                List.of(AppointmentStatus.CANCELLED)
        );
    }
}
