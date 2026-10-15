package com.example.scheduler.dto.appointment;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @Size(max = 500) String reason
) {}
