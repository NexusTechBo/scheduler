package com.example.scheduler.service.impl;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.ScheduleException;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.ScheduleExceptionMapper;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.service.ScheduleExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleExceptionServiceImpl implements ScheduleExceptionService {
    private static final String DEFAULT_CANCELLATION_REASON = "scheduleException.defaultCancellationReason";

    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final PersonalRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final ScheduleExceptionMapper mapper;
    private final Clock clock;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public ScheduleExceptionResponse addDoctorScheduleException(Long doctorId, ScheduleExceptionRequest request) {
        var doctor = doctorRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("personal.doctorNotFound", doctorId));
        if (request.date().isBefore(LocalDate.now(clock)))
            throw new BadRequestException("scheduleException.date.past");
        var affected = findAffectedAppointments(doctorId, request);
        if (!affected.isEmpty() && !Boolean.TRUE.equals(request.cancelAppointments()))
            throw new BusinessException("scheduleException.activeAppointmentsConflict", affected.size());
        var fullDay = request.isFullDayBlock();
        var scheduleException = scheduleExceptionRepository.save(ScheduleException.builder()
                .doctor(doctor)
                .date(request.date())
                .startTime(fullDay ? null : request.startTime())
                .endTime(fullDay ? null : request.endTime())
                .isFullDayBlock(fullDay)
                .reason(request.reason())
                .build());
        if (!affected.isEmpty())
            appointmentService.cancelAppointments(affected, cancellationReason(request.reason()));
        return mapper.toResponse(scheduleException);
    }

    @Override
    public List<ScheduleExceptionResponse> findDoctorScheduleExceptions(Long doctorId, LocalDate from) {
        var schedulesException = scheduleExceptionRepository.findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(doctorId, from);
        return mapper.toResponseList(schedulesException);
    }

    @Override
    @Transactional
    public void deleteDoctorScheduleExceptionById(Long scheduleExceptionId, Long doctorId) {
        var scheduleException = scheduleExceptionRepository.findById(scheduleExceptionId)
                .orElseThrow(() -> new ResourceNotFoundException("scheduleException.notFound", scheduleExceptionId));
        if (!scheduleException.getDoctor().getId().equals(doctorId))
            throw new ForbiddenException("common.forbidden");
        scheduleExceptionRepository.delete(scheduleException);
    }

    private List<Appointment> findAffectedAppointments(Long doctorId, ScheduleExceptionRequest request) {
        var date = request.date();
        var from = request.isFullDayBlock() ? date.atStartOfDay() : date.atTime(request.startTime());
        var to = request.isFullDayBlock() ? date.plusDays(1).atStartOfDay() : date.atTime(request.endTime());
        var now = LocalDateTime.now(clock);
        if (from.isBefore(now))
            from = now;
        if (!from.isBefore(to))
            return List.of();
        return appointmentRepository.findOverlappingAppointments(doctorId, from, to, List.of(AppointmentStatus.CANCELLED));
    }

    private String cancellationReason(String reason) {
        return reason == null || reason.isBlank()
                ? messageSource.getMessage(DEFAULT_CANCELLATION_REASON, null, LocaleContextHolder.getLocale())
                : reason;
    }
}
