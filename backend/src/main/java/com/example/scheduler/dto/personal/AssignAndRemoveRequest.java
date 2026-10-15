package com.example.scheduler.dto.personal;

import jakarta.validation.constraints.NotNull;

public record AssignAndRemoveRequest(
        @NotNull
        Long patientId,
        @NotNull
        Long doctorId
) {}
