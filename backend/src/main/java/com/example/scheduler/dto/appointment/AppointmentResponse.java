package com.example.scheduler.dto.appointment;

import com.example.scheduler.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,

        // Schedule
        LocalDateTime startTime,
        LocalDateTime endTime,

        // Doctor
        Long doctorId,
        String doctorName,
        String doctorSpecialty,
        String doctorEmail,

        // Client
        Long patientId,
        String patientName,
        String patientEmail,

        AppointmentStatus status,
        String cancellationReason,
        LocalDateTime createdAt
) {}
