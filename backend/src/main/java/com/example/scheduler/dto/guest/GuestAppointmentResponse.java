package com.example.scheduler.dto.guest;

import com.example.scheduler.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record GuestAppointmentResponse(
        Long appointmentId,
        String doctorName,
        String specialtyName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status
) {}
