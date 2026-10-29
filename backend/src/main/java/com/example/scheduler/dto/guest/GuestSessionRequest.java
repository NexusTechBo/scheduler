package com.example.scheduler.dto.guest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GuestSessionRequest(
        @NotNull
        @Pattern(regexp = "^[a-z0-9]{3,50}$", message = "{validation.slug.pattern}")
        String slug
) {}
