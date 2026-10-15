package com.example.scheduler.dto.DoctorAvailability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DoctorAvailabilityResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Boolean active
) {}
