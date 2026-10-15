package com.example.scheduler.dto.clinic;

public record ClinicCreatedResponse(
        Long id,
        String name,
        String phoneNumber,
        Long adminPersonalId,
        String adminEmail
) {}
