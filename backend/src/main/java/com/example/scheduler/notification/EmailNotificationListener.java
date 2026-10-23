package com.example.scheduler.notification;

import com.example.scheduler.config.AsyncConfig;
import com.example.scheduler.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
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
    public void onAppointmentNotification(AppointmentNotificationEvent event) {
        switch (event.status()) {
            case CONFIRMED -> emailService.sendAppointmentConfirmed(event);
            case CANCELLED -> emailService.sendAppointmentCancelled(event);
            default -> log.debug("No email for appointment {} with status {}", event.appointmentId(), event.status());
        }
    }
}
