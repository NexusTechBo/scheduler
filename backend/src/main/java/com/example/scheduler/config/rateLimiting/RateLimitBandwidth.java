package com.example.scheduler.config.rateLimiting;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

public record RateLimitBandwidth(
        @Positive long capacity, @NotNull Duration period) {}
