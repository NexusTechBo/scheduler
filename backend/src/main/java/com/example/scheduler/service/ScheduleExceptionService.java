package com.example.scheduler.service;

import com.example.scheduler.dto.scheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.scheduleException.ScheduleExceptionResponse;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleExceptionService {
    ScheduleExceptionResponse addDoctorScheduleException(Long doctorId, ScheduleExceptionRequest request);

    List<ScheduleExceptionResponse> findDoctorScheduleExceptions(Long doctorId, LocalDate from);

    void deleteDoctorScheduleExceptionById(Long scheduleExceptionId, Long doctorId);
}
