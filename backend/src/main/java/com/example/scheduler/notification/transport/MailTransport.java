package com.example.scheduler.notification.transport;

import com.example.scheduler.notification.OutgoingEmail;

public interface MailTransport {
    void send(OutgoingEmail email);
}
