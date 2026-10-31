package com.example.scheduler.service.impl;

import com.example.scheduler.dto.scheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.scheduleException.ScheduleExceptionResponse;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.ScheduleException;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.ScheduleExceptionMapper;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.service.ScheduleExceptionService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleExceptionServiceImpl implements ScheduleExceptionService {
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
        var doctor = doctorRepository
                .findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, doctorId));
        if (request.date().isBefore(LocalDate.now(clock)))
            throw new BadRequestException(MessageKeys.SCHEDULE_EXCEPTION_DATE_PAST);
        var affected = findAffectedAppointments(doctorId, request);
        if (!affected.isEmpty() && !Boolean.TRUE.equals(request.cancelAppointments()))
            throw new BusinessException(MessageKeys.SCHEDULE_EXCEPTION_ACTIVE_APPOINTMENTS_CONFLICT, affected.size());
        var fullDay = request.isFullDayBlock();
        var scheduleException = scheduleExceptionRepository.save(ScheduleException.builder()
                .doctor(doctor)
                .date(request.date())
                .startTime(fullDay ? null : request.startTime())
                .endTime(fullDay ? null : request.endTime())
                .isFullDayBlock(fullDay)
                .reason(request.reason())
                .build());
        if (!affected.isEmpty()) appointmentService.cancelAppointments(affected, cancellationReason(request.reason()));
        return mapper.toResponse(scheduleException);
    }

    @Override
    public List<ScheduleExceptionResponse> findDoctorScheduleExceptions(Long doctorId, LocalDate from) {
        var schedulesException =
                scheduleExceptionRepository.findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                        doctorId, from);
        return mapper.toResponseList(schedulesException);
    }

    @Override
    @Transactional
    public void deleteDoctorScheduleExceptionById(Long scheduleExceptionId, Long doctorId) {
        var scheduleException = scheduleExceptionRepository
                .findById(scheduleExceptionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(MessageKeys.SCHEDULE_EXCEPTION_NOT_FOUND, scheduleExceptionId));
        if (!scheduleException.getDoctor().getId().equals(doctorId))
            throw new ForbiddenException(MessageKeys.COMMON_FORBIDDEN);
        scheduleExceptionRepository.delete(scheduleException);
    }

    private List<Appointment> findAffectedAppointments(Long doctorId, ScheduleExceptionRequest request) {
        var date = request.date();
        var from = request.isFullDayBlock() ? date.atStartOfDay() : date.atTime(request.startTime());
        var to = request.isFullDayBlock() ? date.plusDays(1).atStartOfDay() : date.atTime(request.endTime());
        var now = LocalDateTime.now(clock);
        if (from.isBefore(now)) from = now;
        if (!from.isBefore(to)) return List.of();
        return appointmentRepository.findOverlappingAppointments(
                doctorId, from, to, List.of(AppointmentStatus.CANCELLED));
    }

    private String cancellationReason(String reason) {
        return reason == null || reason.isBlank()
                ? messageSource.getMessage(
                        MessageKeys.SCHEDULE_EXCEPTION_DEFAULT_CANCELLATION_REASON,
                        null,
                        LocaleContextHolder.getLocale())
                : reason;
    }
}
