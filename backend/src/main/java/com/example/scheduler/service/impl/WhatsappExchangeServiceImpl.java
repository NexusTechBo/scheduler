package com.example.scheduler.service.impl;

import com.example.scheduler.dto.whatsapp.WhatsappStatusResponse;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ServiceUnavailableException;
import com.example.scheduler.exchange.WhatsappExchange;
import com.example.scheduler.exchange.contract.MessageRequest;
import com.example.scheduler.exchange.contract.SessionStatus;
import com.example.scheduler.service.WhatsappExchangeService;
import com.example.scheduler.tenant.TenantContext;
import com.example.scheduler.util.Utils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappExchangeServiceImpl implements WhatsappExchangeService {
    private final WhatsappExchange whatsappExchange;
    private final CurrentClinicProvider currentClinicProvider;

    @Override
    public Optional<byte[]> getSessionQr() {
        var sessionId = currentSessionId();
        try {
            return Optional.ofNullable(whatsappExchange.getSessionQr(sessionId)).filter(qr -> qr.length > 0);
        } catch (RestClientResponseException e) {
            if (hasStatus(e, HttpStatus.TOO_MANY_REQUESTS)) {
                log.warn("WhatsApp QR refused: session limit reached on the WhatsApp service");
                throw new ServiceUnavailableException(MessageKeys.WHATSAPP_SESSION_LIMIT);
            }
            throw unavailable("qr", e);
        } catch (RestClientException e) {
            throw unavailable("qr", e);
        }
    }

    @Override
    public WhatsappStatusResponse getSessionStatus() {
        var sessionId = currentSessionId();
        try {
            var status = whatsappExchange.getSessionStatus(sessionId);
            return toStatusResponse(sessionId, status != null ? status.status() : SessionStatus.IDLE);
        } catch (RestClientResponseException e) {
            if (hasStatus(e, HttpStatus.NOT_FOUND)) return toStatusResponse(sessionId, SessionStatus.IDLE);
            throw unavailable("status", e);
        } catch (RestClientException e) {
            throw unavailable("status", e);
        }
    }

    @Override
    public void logoutSession() {
        var sessionId = currentSessionId();
        try {
            whatsappExchange.logoutSession(sessionId);
            log.info("WhatsApp session unlinked");
        } catch (RestClientResponseException e) {
            if (hasStatus(e, HttpStatus.NOT_FOUND) || hasStatus(e, HttpStatus.CONFLICT))
                throw new BusinessException(MessageKeys.WHATSAPP_NO_ACTIVE_SESSION);
            throw unavailable("logout", e);
        } catch (RestClientException e) {
            throw unavailable("logout", e);
        }
    }

    @Override
    public void sendMessage(String sessionId, String to, String message, String ref) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(to)) {
            log.info("WhatsApp message skipped: missing session or phone number [{}]", ref);
            return;
        }
        try {
            whatsappExchange.sendMessage(new MessageRequest(sessionId, Utils.stripLeadingPlus(to), message));
            log.info("WhatsApp message sent [{}]", ref);
        } catch (RestClientResponseException e) {
            log.warn(
                    "WhatsApp message failed [{}]: status {}",
                    ref,
                    e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("WhatsApp message failed [{}]: {}", ref, e.getClass().getSimpleName());
        }
    }

    private static String currentSessionId() {
        var tenant = TenantContext.current();
        if (tenant == null) throw new IllegalStateException("No clinic tenant bound");
        return tenant.clinicSlug();
    }

    private WhatsappStatusResponse toStatusResponse(String sessionId, SessionStatus status) {
        return new WhatsappStatusResponse(
                sessionId, status, currentClinicProvider.getCurrentClinic().getPhoneNumber());
    }

    private static boolean hasStatus(RestClientResponseException e, HttpStatus status) {
        return e.getStatusCode().isSameCodeAs(status);
    }

    private static ServiceUnavailableException unavailable(String operation, RestClientException e) {
        if (e instanceof RestClientResponseException response) {
            log.warn(
                    "WhatsApp service {} failed: status {}",
                    operation,
                    response.getStatusCode().value());
        } else {
            log.warn("WhatsApp service {} failed: {}", operation, e.getClass().getSimpleName());
        }
        return new ServiceUnavailableException(MessageKeys.WHATSAPP_UNAVAILABLE);
    }
}
