package com.example.scheduler.dto.clinic;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicRequest(
        @NotNull
        String name,
        @NotNull
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
