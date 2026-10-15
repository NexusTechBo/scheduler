package com.example.scheduler.dto.appointment;

import jakarta.validation.constraints.Size;

/** Optional body of PATCH /api/appointments/{id}/cancel; the reason is shown to the patient. */
public record CancelAppointmentRequest(
        @Size(max = 500) String reason
) {}
