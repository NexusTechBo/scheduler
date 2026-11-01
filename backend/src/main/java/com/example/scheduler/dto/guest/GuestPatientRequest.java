package com.example.scheduler.dto.guest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestPatientRequest(
        @NotBlank @Size(max = 255) String ci,
        @NotBlank @Size(max = 255) String phoneNumber) {}
