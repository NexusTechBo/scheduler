package com.example.scheduler.dto.DoctorAvailability;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DoctorAvailabilitySlotsResponse(
        LocalDate date,
        Long doctorId,
        List<Slot> availableSlots
) {
    public record Slot(LocalTime startTime, LocalTime endTime) {}
}
