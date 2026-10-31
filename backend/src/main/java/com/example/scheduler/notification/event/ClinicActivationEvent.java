package com.example.scheduler.notification.event;

import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;

public record ClinicActivationEvent(
        Long clinicId, String clinicName, String adminName, String adminEmail, String token, LocalDateTime expiresAt) {
    @Override
    public @NonNull String toString() {
        return "ClinicActivationEvent[clinicId=" + clinicId + ", clinicName=" + clinicName + ", adminEmail="
                + adminEmail + ", expiresAt=" + expiresAt + ", token=****]";
    }
}
