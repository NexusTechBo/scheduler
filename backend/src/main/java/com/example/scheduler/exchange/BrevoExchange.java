package com.example.scheduler.exchange;

import com.example.scheduler.exchange.contract.BrevoEmailRequest;
import com.example.scheduler.exchange.contract.BrevoEmailResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/v3")
public interface BrevoExchange {
    @PostExchange("/smtp/email")
    BrevoEmailResponse sendEmail(@RequestBody BrevoEmailRequest request);
}
