package com.example.scheduler.notification.event;

import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentNotificationEvent(
        AppointmentStatus status,
        Long appointmentId,
        String clinicName,
        String clinicPhoneNumber,
        String doctorName,
        String specialtyName,
        String patientName,
        String patientEmail,
        String patientPhoneNumber,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String cancellationReason) {}
