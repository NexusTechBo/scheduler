package com.example.scheduler.dto.scheduleexception;

import com.example.scheduler.validator.partialBlockTimes.PartialBlockTimes;
import com.example.scheduler.validator.timeRange.TimeRange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

@TimeRange
@PartialBlockTimes
public record ScheduleExceptionRequest(
        @NotNull LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        @NotNull Boolean isFullDayBlock,
        @Size(max = 255) String reason,
        Boolean cancelAppointments)
        implements TimeRange.Request {}
