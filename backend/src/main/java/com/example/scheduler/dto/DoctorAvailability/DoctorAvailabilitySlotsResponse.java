package com.example.scheduler.dto.DoctorAvailability;

import java.time.LocalDate;
import java.util.List;

public record DoctorAvailabilitySlotsResponse(
        LocalDate date,
        Long doctorId,
        List<SlotResponse> availableSlots
) {}
