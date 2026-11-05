package com.example.scheduler.dto.appointment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(@NotNull LocalDateTime startTime) {}
