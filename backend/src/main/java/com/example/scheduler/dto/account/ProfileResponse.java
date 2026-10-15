package com.example.scheduler.dto.account;

import com.example.scheduler.enums.ERole;

public record ProfileResponse(
        Long id,
        String ci,
        String name,
        String email,
        String phoneNumber,
        ERole role,
        String specialtyName
) {}
