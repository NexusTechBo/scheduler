package com.example.scheduler.dto.doctoravailability;

import java.time.LocalDate;
import java.util.List;

public record DoctorAvailabilitySlotsResponse(LocalDate date, Long doctorId, List<SlotResponse> availableSlots) {}
