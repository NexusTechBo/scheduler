package com.example.scheduler.dto.ScheduleException;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionResponse(
        Long id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Boolean isFullDayBlock,
        String reason
) {}
