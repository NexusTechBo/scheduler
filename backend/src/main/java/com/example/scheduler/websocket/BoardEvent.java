package com.example.scheduler.websocket;

import com.example.scheduler.enums.AppointmentStatus;

/** Payload pushed to {@code /topic/clinic/{clinicId}/board} when an appointment is created or changes status. */
public record BoardEvent(Long appointmentId, AppointmentStatus status) {}
