package com.example.scheduler.service;

import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.ClinicActivatedEvent;
import com.example.scheduler.notification.event.ClinicActivationEvent;
import com.example.scheduler.notification.event.StaffCreatedEvent;

public interface EmailService {
    void sendStaffCredentials(StaffCreatedEvent event);

    void sendAppointmentConfirmed(AppointmentNotificationEvent event);

    void sendAppointmentCancelled(AppointmentNotificationEvent event);

    void sendClinicActivation(ClinicActivationEvent event);

    void sendClinicWelcome(ClinicActivatedEvent event);
}
