package com.example.scheduler.dto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientRequest(
        @NotBlank @Pattern(regexp = "^\\d{5,10}$", message = "{validation.ci.pattern}")
        String ci,

        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String phoneNumber) {}
