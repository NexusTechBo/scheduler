package com.example.scheduler.service;

import com.example.scheduler.exchange.contract.SessionStatusResponse;
import java.util.Optional;

public interface WhatsappExchangeService {
    Optional<byte[]> getSessionQr();

    SessionStatusResponse getSessionStatus();

    void logoutSession();

    void sendMessage(String clinicPhoneNumber, String to, String message, String ref);
}
