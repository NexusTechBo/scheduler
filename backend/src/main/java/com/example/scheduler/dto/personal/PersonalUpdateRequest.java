package com.example.scheduler.dto.personal;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.validator.role.ValidRole;
import com.example.scheduler.validator.specialtyRole.SpecialtyRoleMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SpecialtyRoleMatch
public record PersonalUpdateRequest(
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        String phoneNumber,
        @Size(min = 8)
        String password,
        @NotNull
        @ValidRole
        ERole role,
        Long specialtyId
) implements SpecialtyRoleMatch.Request {}
