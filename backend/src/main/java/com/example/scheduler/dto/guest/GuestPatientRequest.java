package com.example.scheduler.dto.guest;

import jakarta.validation.constraints.NotBlank;

public record GuestPatientRequest(
        @NotBlank
        String ci,
        @NotBlank
        String phoneNumber
) {}
