package com.example.scheduler.dto.clinic;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicRequest(
        @NotBlank
        String name,
        @NotBlank
        String phoneNumber,
        @NotBlank
        String adminName,
        @NotBlank
        @Email
        String adminEmail,
        @NotBlank
        @Size(min = 8)
        String adminPassword,
        @NotBlank
        String adminCi
) {}
