package com.example.scheduler.dto.clinic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateClinicRequest(
        @NotBlank
        @Size(max = 255)
        String token
) {}
