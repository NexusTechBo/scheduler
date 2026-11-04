package com.example.scheduler.notification.event;

import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public record AppointmentNotificationEvent(
        AppointmentStatus status,
        Long appointmentId,
        String clinicName,
        String clinicSlug,
        String doctorName,
        String specialtyName,
        String patientName,
        String patientEmail,
        String patientPhoneNumber,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String cancellationReason,
        Set<NotificationChannel> channels) {
    public static final Set<NotificationChannel> ALL_CHANNELS = Set.of(NotificationChannel.values());
    public static final Set<NotificationChannel> WHATSAPP_ONLY = Set.of(NotificationChannel.WHATSAPP);

    public AppointmentNotificationEvent {
        channels = Set.copyOf(Objects.requireNonNull(channels, "channels"));
    }

    public boolean targets(NotificationChannel channel) {
        return channels.contains(channel);
    }
}
