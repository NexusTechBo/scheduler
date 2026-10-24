package com.example.scheduler.dto.clinic;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendActivationRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email
) {}
