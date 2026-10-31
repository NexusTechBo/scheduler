package com.example.scheduler.notification.event;

import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentNotificationEvent(
        AppointmentStatus status,
        Long appointmentId,
        String clinicName,
        String doctorName,
        String specialtyName,
        String patientName,
        String patientEmail,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String cancellationReason) {}
