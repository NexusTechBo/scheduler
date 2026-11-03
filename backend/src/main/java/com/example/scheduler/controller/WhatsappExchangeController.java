package com.example.scheduler.controller;

import com.example.scheduler.exchange.contract.SessionStatusResponse;
import com.example.scheduler.security.RoleExpressions;
import com.example.scheduler.service.WhatsappExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp", description = "Link the clinic's WhatsApp used to notify patients")
@PreAuthorize(RoleExpressions.ADMINISTRATOR)
public class WhatsappExchangeController {
    private final WhatsappExchangeService whatsappExchangeService;

    @Operation(summary = "QR code (PNG) to link the clinic's WhatsApp; 204 when it is already linked")
    @GetMapping("/qr")
    public ResponseEntity<byte[]> getSessionQr() {
        return whatsappExchangeService
                .getSessionQr()
                .map(qr -> ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qr))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Status of the clinic's WhatsApp session")
    @GetMapping("/status")
    public SessionStatusResponse getSessionStatus() {
        return whatsappExchangeService.getSessionStatus();
    }

    @Operation(summary = "Unlink the clinic's WhatsApp")
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutSession() {
        whatsappExchangeService.logoutSession();
        return ResponseEntity.noContent().build();
    }
}
