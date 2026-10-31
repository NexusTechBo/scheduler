package com.example.scheduler.dto.doctorAvailability;

import java.time.LocalTime;

public record SlotResponse(LocalTime startTime, LocalTime endTime) {}
