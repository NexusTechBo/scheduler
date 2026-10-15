package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
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
import org.springframework.util.StringUtils;

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

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, Long userId, String role) {
        // A patient can only book for itself. Every new appointment starts PENDING until staff confirms it.
        var isPatient = ERole.PATIENT.name().equals(role);
        var patientId = isPatient ? userId : request.patientId();
        var startTime = request.startTime();
        if (isBlocked(request.doctorId(), startTime.toLocalDate()))
            throw new BusinessException("That schedule is blocked");
        var endTime = findSlotEnd(request.doctorId(), startTime)
                .orElseThrow(() -> new BusinessException("That Schedule is out of journey"));
        if (isSlotTaken(request.doctorId(), startTime, endTime))
            throw new BusinessException("That slot is already taken");
        var doctor = personalRepository.getReferenceById(request.doctorId());
        var patient = patientRepository.getReferenceById(patientId);
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
        appointment.setCancellationReason(StringUtils.hasText(request.reason()) ? request.reason().strip() : null);
        return appointmentMapper.toResponse(saveAndNotify(appointment));
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

    /** Saves and publishes a board event, pushed to the clinic's subscribers after commit. */
    private Appointment saveAndNotify(Appointment appointment) {
        var saved = appointmentRepository.save(appointment);
        // The tenant is "clinic_<id>" (set by JwtAuthFilter), so the clinic id is known without another query.
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

    private boolean isBlocked(Long doctorId, LocalDate date) {
        return scheduleExceptionRepository.existsByDoctorIdAndDateAndIsFullDayBlockTrue(
                doctorId, date
        );
    }

    /**
     * End of the slot starting at {@code startTime}: an active block of that day must contain it on its slot grid
     * ((start - block start) % slot == 0) with the whole slot before the block ends. Empty when no block matches.
     */
    private Optional<LocalDateTime> findSlotEnd(Long doctorId, LocalDateTime startTime) {
        var time = startTime.toLocalTime();
        return doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, startTime.getDayOfWeek()).stream()
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
