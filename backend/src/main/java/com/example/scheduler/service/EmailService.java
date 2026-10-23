package com.example.scheduler.service;

import com.example.scheduler.notification.AppointmentNotificationEvent;
import com.example.scheduler.notification.StaffCreatedEvent;

public interface EmailService {
    void sendStaffCredentials(StaffCreatedEvent event);
    void sendAppointmentConfirmed(AppointmentNotificationEvent event);
    void sendAppointmentCancelled(AppointmentNotificationEvent event);
}
