package com.example.scheduler.notification;

public record ClinicActivatedEvent(
        Long clinicId,
        String clinicName,
        String adminName,
        String adminEmail,
        String adminUsername
) {}
