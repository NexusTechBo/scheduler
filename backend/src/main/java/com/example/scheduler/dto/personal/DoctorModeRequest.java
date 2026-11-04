package com.example.scheduler.dto.personal;

import jakarta.validation.constraints.NotNull;

public record DoctorModeRequest(@NotNull Boolean enabled, Long specialtyId) {}
