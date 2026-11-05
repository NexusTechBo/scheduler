package com.example.scheduler.dto.guest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestNewPatientRequest(
        @NotBlank @Pattern(regexp = "^\\d{5,10}$", message = "{validation.ci.pattern}")
        String ci,

        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String phoneNumber,
        @Email @Size(max = 255) String email) {}
