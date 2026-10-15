package com.example.scheduler.dto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRegisterRequest(
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        String ci,
        @NotBlank
        @Size(min = 8)
        String password,
        String phoneNumber
) {}
