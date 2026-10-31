package com.example.scheduler.notification;

import com.example.scheduler.config.AsyncConfig;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.ClinicActivatedEvent;
import com.example.scheduler.notification.event.ClinicActivationEvent;
import com.example.scheduler.notification.event.StaffCreatedEvent;
import com.example.scheduler.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailNotificationListener {
    private final EmailService emailService;

    @Async(AsyncConfig.MAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStaffCreated(StaffCreatedEvent event) {
        emailService.sendStaffCredentials(event);
    }

    @Async(AsyncConfig.MAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClinicActivation(ClinicActivationEvent event) {
        emailService.sendClinicActivation(event);
    }

    @Async(AsyncConfig.MAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClinicActivated(ClinicActivatedEvent event) {
        emailService.sendClinicWelcome(event);
    }

    @Async(AsyncConfig.MAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentNotification(AppointmentNotificationEvent event) {
        switch (event.status()) {
            case CONFIRMED -> emailService.sendAppointmentConfirmed(event);
            case CANCELLED -> emailService.sendAppointmentCancelled(event);
        }
    }
}
