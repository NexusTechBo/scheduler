package com.example.scheduler.dto.personal;

public record PersonalResponse(
        Long id,
        String username,
        String ci,
        String name,
        String email,
        String phoneNumber,
        boolean active,
        boolean alsoDoctor,
        String role,
        Long specialtyId,
        String specialtyName) {}
