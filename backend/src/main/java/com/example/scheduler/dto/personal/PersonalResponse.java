package com.example.scheduler.dto.personal;

public record PersonalResponse(
        Long id,
        String username,
        String ci,
        String name,
        String email,
        String phoneNumber,
        boolean active,
        String role,
        String specialtyName
) {}
