package com.example.scheduler.dto.DoctorAvailability;

import java.time.LocalTime;

public record SlotResponse(
        LocalTime startTime,
        LocalTime endTime
) {}
