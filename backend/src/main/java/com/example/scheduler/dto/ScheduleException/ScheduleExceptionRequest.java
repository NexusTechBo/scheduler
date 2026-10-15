package com.example.scheduler.dto.ScheduleException;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionRequest(
        @NotNull
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        @NotNull
        Boolean isFullDayBlock,
        String reason
) {}
