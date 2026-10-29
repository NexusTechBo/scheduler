package com.example.scheduler.dto.guest;

import com.example.scheduler.dto.patient.PatientRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record GuestAppointmentRequest(
        @NotNull
        Long doctorId,
        @NotNull
        LocalDateTime startTime,
        @NotNull
        @Valid
        PatientRequest patient
) {}
