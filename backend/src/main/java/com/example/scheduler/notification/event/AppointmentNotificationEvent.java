package com.example.scheduler.notification.event;

import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public record AppointmentNotificationEvent(
        AppointmentNotificationType type,
        AppointmentStatus status,
        Long appointmentId,
        String clinicName,
        String clinicSlug,
        String doctorName,
        String doctorEmail,
        String specialtyName,
        String patientName,
        String patientEmail,
        String patientPhoneNumber,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String cancellationReason,
        String reason,
        Set<NotificationChannel> channels) {

    public static final Set<NotificationChannel> ALL_CHANNELS = Set.of(NotificationChannel.values());
    public static final Set<NotificationChannel> WHATSAPP_ONLY = Set.of(NotificationChannel.WHATSAPP);
    public static final Set<NotificationChannel> EMAIL_ONLY = Set.of(NotificationChannel.EMAIL);

    public AppointmentNotificationEvent {
        Objects.requireNonNull(type, "type");
        channels = Set.copyOf(Objects.requireNonNull(channels, "channels"));
    }

    // Constructor de retrocompatibilidad para llamadas sin doctorEmail ni reason
    public AppointmentNotificationEvent(
            AppointmentNotificationType type,
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
        this(
                type,
                status,
                appointmentId,
                clinicName,
                clinicSlug,
                doctorName,
                null,
                specialtyName,
                patientName,
                patientEmail,
                patientPhoneNumber,
                startTime,
                endTime,
                cancellationReason,
                null,
                channels);
    }

    public boolean targets(NotificationChannel channel) {
        return channels.contains(channel);
    }
}