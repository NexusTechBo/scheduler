package com.example.scheduler.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentChanged(AppointmentChangedEvent event) {
        var appointmentId = event.payload().appointmentId();
        try {
            messagingTemplate.convertAndSend(AppointmentChangedEvent.boardTopic(event.clinicId()), event.payload());
        } catch (RuntimeException e) {
            log.error(
                    "Board event for appointment {} could not be published to clinic {}",
                    appointmentId,
                    event.clinicId(),
                    e);
        }
    }
}
