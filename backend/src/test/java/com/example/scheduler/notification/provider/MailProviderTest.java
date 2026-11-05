package com.example.scheduler.notification.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MailProviderTest {
    @Test
    void shouldUseBrevoAsDefault() {
        assertThat(MailProvider.DEFAULT).isEqualTo(MailProvider.BREVO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldResolveDefaultWhenPropertyIsBlank(String raw) {
        assertThat(MailProvider.fromProperty(raw)).contains(MailProvider.DEFAULT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"brevo", "BREVO", "BrEvO", "  brevo  "})
    void shouldResolveBrevoIgnoringCaseAndSurroundingWhitespace(String raw) {
        assertThat(MailProvider.fromProperty(raw)).contains(MailProvider.BREVO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"smtp", "SMTP", " Smtp "})
    void shouldResolveSmtpIgnoringCaseAndSurroundingWhitespace(String raw) {
        assertThat(MailProvider.fromProperty(raw)).contains(MailProvider.SMTP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sendgrid", "brevo-api", "s m t p"})
    void shouldResolveEmptyWhenPropertyIsUnknown(String raw) {
        assertThat(MailProvider.fromProperty(raw)).isEmpty();
    }
}
