package com.example.scheduler.dto.account;

public record AccountResponse(
        Long id,
        String ci,
        String name,
        String email,
        String phoneNumber
) {}
