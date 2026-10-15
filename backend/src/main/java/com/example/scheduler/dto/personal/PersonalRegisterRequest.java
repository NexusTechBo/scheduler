package com.example.scheduler.dto.personal;

import com.example.scheduler.validator.role.ValidRole;
import com.example.scheduler.validator.specialtyRole.SpecialtyRoleMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SpecialtyRoleMatch(
        first = "roleId",
        second = "specialtyId"
)
public record PersonalRegisterRequest(
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
        @NotNull
        @ValidRole
        Long roleId,
        Long specialtyId
) {}
