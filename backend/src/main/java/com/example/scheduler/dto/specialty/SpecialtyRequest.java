package com.example.scheduler.dto.specialty;

import jakarta.validation.constraints.NotBlank;

public record SpecialtyRequest(
        @NotBlank
        String name
) {}
