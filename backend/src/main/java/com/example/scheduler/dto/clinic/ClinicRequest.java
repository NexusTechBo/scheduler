package com.example.scheduler.dto.clinic;

import com.example.scheduler.validator.maxUtf8Bytes.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClinicRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "{validation.noControlChars}")
        String name,

        @NotNull @Pattern(regexp = "^[a-z0-9]{3,50}$", message = "{validation.slug.pattern}")
        String slug,

        @NotBlank @Size(max = 255) String phoneNumber,
        @NotBlank @Size(max = 255) String adminName,

        @NotNull @Pattern(regexp = "^[a-z0-9._-]{3,50}$", message = "{validation.username.pattern}")
        String adminUsername,

        @NotBlank @Email @Size(max = 255) String adminEmail,
        @NotBlank @Size(min = 8, max = 72) @MaxUtf8Bytes String adminPassword,
        @NotBlank @Size(max = 255) String adminCi) {}
