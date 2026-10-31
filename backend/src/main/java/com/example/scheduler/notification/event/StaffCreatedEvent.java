package com.example.scheduler.notification.event;

import com.example.scheduler.enums.ERole;
import org.jspecify.annotations.NonNull;

public record StaffCreatedEvent(
        String email, String name, String username, String rawPassword, ERole role, String clinicName) {
    @Override
    public @NonNull String toString() {
        return "StaffCreatedEvent[email=" + email + ", username=" + username + ", role=" + role + ", clinicName="
                + clinicName + ", rawPassword=****]";
    }
}
