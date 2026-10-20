package com.example.scheduler.dto.clinic;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClinicRequest(
        @NotBlank
        String name,
        @NotNull
        @Pattern(regexp = "^[a-z0-9]{3,50}$", message = "must be 3-50 characters: lowercase letters or digits")
        String subdomain,
        @NotBlank
        String phoneNumber,
        @NotBlank
        String adminName,
        @NotNull
        @Pattern(regexp = "^[a-z0-9._-]{3,50}$", message = "must be 3-50 characters: lowercase letters, digits, '.', '_' or '-'")
        String adminUsername,
        @NotBlank
        @Email
        String adminEmail,
        @NotBlank
        @Size(min = 8)
        String adminPassword,
        @NotBlank
        String adminCi
) {}
