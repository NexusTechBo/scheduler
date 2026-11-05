package com.example.scheduler.notification.event;

/** What happened to the appointment; decides which patient template is sent. */
public enum AppointmentNotificationType {
    CONFIRMED,
    CANCELLED,
    RESCHEDULED
}
