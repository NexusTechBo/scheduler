package com.example.scheduler.dto.personal;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.validator.role.ValidRole;
import com.example.scheduler.validator.specialtyRole.SpecialtyRoleMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@SpecialtyRoleMatch(
        first = "role",
        second = "specialtyId"
)
public record PersonalRegisterRequest(
        @NotNull
        @Pattern(regexp = "^[a-z0-9._-]{3,50}$", message = "must be 3-50 characters: lowercase letters, digits, '.', '_' or '-'")
        String username,
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        String ci,
        @NotBlank
        @Size(min = 8)
        String password,
        String phoneNumber,
        @NotNull
        @ValidRole
        ERole role,
        Long specialtyId
) {}
