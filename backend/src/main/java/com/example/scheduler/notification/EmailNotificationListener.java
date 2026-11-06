package com.example.scheduler.notification;

import com.example.scheduler.config.AsyncConfig;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.AppointmentNotificationType;
import com.example.scheduler.notification.event.ClinicActivatedEvent;
import com.example.scheduler.notification.event.ClinicActivationEvent;
import com.example.scheduler.notification.event.NotificationChannel;
import com.example.scheduler.notification.event.StaffCreatedEvent;
import com.example.scheduler.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

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
        if (!event.targets(NotificationChannel.EMAIL)) return;

        if (event.type() == AppointmentNotificationType.DOCTOR_GUEST_BOOKED) {
            if (!StringUtils.hasText(event.doctorEmail())) {
                log.info("Appointment {} doctor email notification skipped: doctor has no email", event.appointmentId());
                return;
            }
            emailService.sendDoctorGuestAppointment(event);
            return;
        }

        if (!StringUtils.hasText(event.patientEmail())) {
            log.info("Appointment {} email notification skipped: patient has no email", event.appointmentId());
            return;
        }

        switch (event.type()) {
            case CONFIRMED -> emailService.sendAppointmentConfirmed(event);
            case CANCELLED -> emailService.sendAppointmentCancelled(event);
            case RESCHEDULED -> emailService.sendAppointmentRescheduled(event);
            default -> log.debug("Unhandled appointment notification type for email: {}", event.type());
        }
    }
}