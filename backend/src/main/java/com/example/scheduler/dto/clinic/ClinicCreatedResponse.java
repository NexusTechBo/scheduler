package com.example.scheduler.dto.clinic;

public record ClinicCreatedResponse(
        Long id,
        String name,
        String phoneNumber,
        String subdomain,
        Long adminPersonalId,
        String adminUsername,
        String adminEmail
) {}
