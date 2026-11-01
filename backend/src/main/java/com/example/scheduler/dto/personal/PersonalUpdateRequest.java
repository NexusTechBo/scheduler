package com.example.scheduler.dto.personal;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.validator.maxUtf8Bytes.MaxUtf8Bytes;
import com.example.scheduler.validator.role.ValidRole;
import com.example.scheduler.validator.specialtyRole.SpecialtyRoleMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SpecialtyRoleMatch
public record PersonalUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 255) String phoneNumber,
        @Size(min = 8, max = 72) @MaxUtf8Bytes String password,
        @NotNull @ValidRole ERole role,
        Long specialtyId)
        implements SpecialtyRoleMatch.Request {}
