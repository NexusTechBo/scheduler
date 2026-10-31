package com.example.scheduler.notification.event;

public record ClinicActivatedEvent(
        Long clinicId, String clinicName, String adminName, String adminEmail, String adminUsername) {}
