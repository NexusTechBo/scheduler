package com.example.scheduler.dto.doctorAvailability;

import java.time.LocalDate;
import java.util.List;

public record DoctorAvailabilitySlotsResponse(LocalDate date, Long doctorId, List<SlotResponse> availableSlots) {}
