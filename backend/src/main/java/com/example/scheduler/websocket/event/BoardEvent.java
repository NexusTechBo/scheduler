package com.example.scheduler.websocket.event;

import com.example.scheduler.enums.AppointmentStatus;

public record BoardEvent(Long appointmentId, AppointmentStatus status) {}
