package com.example.scheduler.dto.patient;

import jakarta.validation.constraints.NotBlank;

public record PatientRequest(
        @NotBlank
        String name,
        String email,
        String phoneNumber
) {}
