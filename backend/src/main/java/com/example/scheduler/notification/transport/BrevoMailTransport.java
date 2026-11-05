package com.example.scheduler.notification.transport;

import com.example.scheduler.config.AppMailProperties;
import com.example.scheduler.exception.MailDeliveryException;
import com.example.scheduler.exchange.BrevoExchange;
import com.example.scheduler.exchange.contract.BrevoContact;
import com.example.scheduler.exchange.contract.BrevoEmailRequest;
import com.example.scheduler.exchange.contract.BrevoErrorResponse;
import com.example.scheduler.notification.OutgoingEmail;
import com.example.scheduler.notification.provider.ConditionalOnMailProvider;
import com.example.scheduler.notification.provider.MailProvider;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@ConditionalOnMailProvider(MailProvider.BREVO)
public class BrevoMailTransport implements MailTransport {
    private static final int MAX_ERROR_DETAIL = 300;

    private final BrevoExchange brevoExchange;
    private final AppMailProperties properties;

    public BrevoMailTransport(BrevoExchange brevoExchange, AppMailProperties properties) {
        this.brevoExchange = brevoExchange;
        this.properties = properties;
        if (!properties.brevo().hasApiKey()) {
            log.warn("Brevo API key is not configured (BREVO_API_KEY); emails will fail until it is set");
        }
        if (!properties.hasFrom()) {
            log.warn("Mail sender is not configured (MAIL_FROM); emails will fail until it is set");
        }
    }

    @Override
    public void send(OutgoingEmail email) {
        if (!properties.brevo().hasApiKey()) {
            throw new MailDeliveryException("Brevo API key not configured");
        }
        if (!properties.hasFrom()) {
            throw new MailDeliveryException("Mail sender (MAIL_FROM) not configured");
        }
        var request = new BrevoEmailRequest(
                new BrevoContact(properties.from(), properties.fromName()),
                List.of(new BrevoContact(email.to(), null)),
                email.subject(),
                email.html());
        try {
            var response = brevoExchange.sendEmail(request);
            log.debug("Brevo accepted email [messageId={}]", response != null ? response.messageId() : null);
        } catch (RestClientResponseException e) {
            throw new MailDeliveryException(
                    "Brevo responded " + e.getStatusCode().value() + ": " + errorDetail(e), e);
        } catch (RestClientException e) {
            throw new MailDeliveryException("Brevo unreachable: " + e.getClass().getSimpleName(), e);
        }
    }

    private static String errorDetail(RestClientResponseException e) {
        String detail = null;
        try {
            var error = e.getResponseBodyAs(BrevoErrorResponse.class);
            if (error != null) detail = error.message();
        } catch (RuntimeException ignored) {
        }
        if (!StringUtils.hasText(detail)) detail = e.getResponseBodyAsString();
        if (!StringUtils.hasText(detail)) detail = e.getStatusText();
        return detail.length() > MAX_ERROR_DETAIL ? detail.substring(0, MAX_ERROR_DETAIL) + "..." : detail;
    }
}
