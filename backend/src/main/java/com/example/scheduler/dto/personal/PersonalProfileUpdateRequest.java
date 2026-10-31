package com.example.scheduler.dto.personal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalProfileUpdateRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phoneNumber,
        String currentPassword,
        @Size(min = 8) String newPassword) {}
