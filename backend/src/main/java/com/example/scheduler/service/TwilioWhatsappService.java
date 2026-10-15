package com.example.scheduler.service;

public interface TwilioWhatsappService {
    String sendWhatsAppMessage(String toPhoneNumber, String messageBody);

}
