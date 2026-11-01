package com.example.scheduler.dto.personal;

import com.example.scheduler.validator.maxUtf8Bytes.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalProfileUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 255) String phoneNumber,
        @MaxUtf8Bytes String currentPassword,
        @Size(min = 8, max = 72) @MaxUtf8Bytes String newPassword) {}
