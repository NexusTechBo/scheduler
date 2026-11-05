package com.example.scheduler.exchange;

import com.example.scheduler.exchange.contract.MessageRequest;
import com.example.scheduler.exchange.contract.SessionStatusResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api")
public interface WhatsappExchange {
    @GetExchange(value = "/sessions/{id}/qr", accept = MediaType.IMAGE_PNG_VALUE)
    byte[] getSessionQr(@PathVariable("id") String id);

    @GetExchange("/sessions/{id}/status")
    SessionStatusResponse getSessionStatus(@PathVariable("id") String id);

    @PostExchange("/sessions/{id}/logout")
    void logoutSession(@PathVariable("id") String id);

    @PostExchange("/messages/send")
    void sendMessage(@RequestBody MessageRequest request);
}
