package com.example.scheduler.dto.guest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record GuestAppointmentRequest<P>(
        @NotNull Long doctorId,
        @NotNull LocalDateTime startTime,
        @NotNull @Valid P patient) {}
