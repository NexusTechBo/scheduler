package com.example.scheduler.dto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRequest(
        @NotBlank @Size(max = 255) String ci,
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String phoneNumber) {}
