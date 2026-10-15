package com.example.scheduler.dto.appointment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull
        Long doctorId,
        @NotNull
        Long patientId,
        @NotNull
        LocalDateTime startTime
) {}
