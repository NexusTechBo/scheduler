package com.example.scheduler.dto.scheduleException;

import com.example.scheduler.validator.partialBlockTimes.PartialBlockTimes;
import com.example.scheduler.validator.timeRange.TimeRange;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@TimeRange
@PartialBlockTimes
public record ScheduleExceptionRequest(
        @NotNull LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        @NotNull Boolean isFullDayBlock,
        String reason,
        Boolean cancelAppointments)
        implements TimeRange.Request {}
