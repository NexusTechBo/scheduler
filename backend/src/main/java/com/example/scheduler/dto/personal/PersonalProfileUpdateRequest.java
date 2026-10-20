package com.example.scheduler.dto.personal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body a staff member sends to update their own profile; {@code newPassword} requires the matching {@code currentPassword}. */
public record PersonalProfileUpdateRequest(
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        String phoneNumber,
        String currentPassword,
        @Size(min = 8)
        String newPassword
) {}
