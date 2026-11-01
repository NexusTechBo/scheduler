package com.example.scheduler.dto.guest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestNewPatientRequest(
        @NotBlank @Size(max = 255) String ci,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String phoneNumber,
        @Email @Size(max = 255) String email) {}
