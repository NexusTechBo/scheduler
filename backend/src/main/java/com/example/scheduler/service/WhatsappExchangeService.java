package com.example.scheduler.service;

import com.example.scheduler.dto.whatsapp.WhatsappStatusResponse;
import java.util.Optional;

public interface WhatsappExchangeService {
    Optional<byte[]> getSessionQr();

    WhatsappStatusResponse getSessionStatus();

    void logoutSession();

    void sendMessage(String sessionId, String to, String message, String ref);
}
