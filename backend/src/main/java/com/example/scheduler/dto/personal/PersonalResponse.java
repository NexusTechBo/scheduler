package com.example.scheduler.dto.personal;

public record PersonalResponse(
        Long id,
        String name,
        String email,
        boolean active,
        String roleName,
        String specialtyName
) {}
