package com.example.scheduler.dto.clinic;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClinicRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "{validation.noControlChars}")
        String name,
        @NotNull
        @Pattern(regexp = "^[a-z0-9]{3,50}$", message = "{validation.subdomain.pattern}")
        String subdomain,
        @NotBlank
        String phoneNumber,
        @NotBlank
        String adminName,
        @NotNull
        @Pattern(regexp = "^[a-z0-9._-]{3,50}$", message = "{validation.username.pattern}")
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
