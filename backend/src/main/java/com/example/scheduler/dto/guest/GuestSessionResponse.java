package com.example.scheduler.dto.guest;

import java.time.Instant;

public record GuestSessionResponse(
        String token,
        Instant expiresAt
) {}
