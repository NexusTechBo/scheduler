package com.example.scheduler.dto.patient;

public record PatientResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        boolean active
) {}
