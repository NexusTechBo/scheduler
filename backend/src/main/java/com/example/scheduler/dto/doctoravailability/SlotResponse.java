package com.example.scheduler.dto.doctoravailability;

import java.time.LocalTime;

public record SlotResponse(LocalTime startTime, LocalTime endTime) {}
