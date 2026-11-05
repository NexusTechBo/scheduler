package com.example.scheduler.notification.provider;

import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;

public enum MailProvider {
    BREVO,
    SMTP;

    public static final MailProvider DEFAULT = BREVO;

    public static Optional<MailProvider> fromProperty(String raw) {
        if (!StringUtils.hasText(raw)) return Optional.of(DEFAULT);
        try {
            return Optional.of(valueOf(raw.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
