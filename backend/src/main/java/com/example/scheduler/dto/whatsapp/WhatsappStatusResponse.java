package com.example.scheduler.dto.whatsapp;

import com.example.scheduler.exchange.contract.SessionStatus;

public record WhatsappStatusResponse(String id, SessionStatus status, String clinicPhoneNumber) {}
