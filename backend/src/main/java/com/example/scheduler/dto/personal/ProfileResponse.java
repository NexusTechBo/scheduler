package com.example.scheduler.dto.personal;

import com.example.scheduler.enums.ERole;

public record ProfileResponse(
        Long id,
        String username,
        String ci,
        String name,
        String email,
        String phoneNumber,
        ERole role,
        String specialtyName
) {}
