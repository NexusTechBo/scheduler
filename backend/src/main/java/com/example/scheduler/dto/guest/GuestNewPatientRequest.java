package com.example.scheduler.dto.guest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GuestNewPatientRequest(
        @NotBlank
        String ci,
        @NotBlank
        String name,
        @NotBlank
        String phoneNumber,
        @Email
        String email
) {}
