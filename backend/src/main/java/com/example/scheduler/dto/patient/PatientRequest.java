package com.example.scheduler.dto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PatientRequest(
        @NotBlank
        String ci,
        @NotBlank
        String name,
        @Email
        String email,
        @NotBlank
        String phoneNumber
) {}
