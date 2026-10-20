package com.example.scheduler.dto.personal;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.validator.role.ValidRole;
import com.example.scheduler.validator.specialtyRole.SpecialtyRoleMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body an administrator sends to update another staff member; {@code password} resets it when present. */
@SpecialtyRoleMatch(
        first = "role",
        second = "specialtyId"
)
public record PersonalUpdateRequest(
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        String phoneNumber,
        Long specialtyId,
        @NotNull
        @ValidRole
        ERole role,
        @Size(min = 8)
        String password
) {}
