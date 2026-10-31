package com.example.scheduler.dto.scheduleException;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionResponse(
        Long id, LocalDate date, LocalTime startTime, LocalTime endTime, Boolean isFullDayBlock, String reason) {}
