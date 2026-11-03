package com.example.scheduler.notification;

import com.example.scheduler.config.AsyncConfig;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.template.WhatsappTemplates;
import com.example.scheduler.service.WhatsappExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WhatsappNotificationListener {
    private final WhatsappExchangeService whatsappExchangeService;

    @Async(AsyncConfig.WHATSAPP_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentNotification(AppointmentNotificationEvent event) {
        var message = switch (event.status()) {
            case CONFIRMED -> WhatsappTemplates.appointmentConfirmed(event);
            case CANCELLED -> WhatsappTemplates.appointmentCancelled(event);
            default -> null;
        };
        if (message == null) return;
        whatsappExchangeService.sendMessage(
                event.clinicPhoneNumber(),
                event.patientPhoneNumber(),
                message,
                "appointment=" + event.appointmentId() + " status=" + event.status());
    }
}
