package com.example.scheduler.dto.personal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PersonalRequest(
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        Long specialtyId
) {}
