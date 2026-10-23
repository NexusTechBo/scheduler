package com.example.scheduler.notification;

import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

public record ClinicActivationEvent(
        Long clinicId,
        String clinicName,
        String adminName,
        String adminEmail,
        String token,
        LocalDateTime expiresAt
) {

    @Override
    public @NonNull String toString() {
        return "ClinicActivationEvent[clinicId=" + clinicId + ", clinicName=" + clinicName + ", adminEmail=" + adminEmail
                + ", expiresAt=" + expiresAt + ", token=****]";
    }
}
