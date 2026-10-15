package com.example.scheduler.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        String password,
        @NotNull
        Long clinicId
) {}
