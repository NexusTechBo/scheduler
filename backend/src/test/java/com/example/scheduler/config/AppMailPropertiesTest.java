package com.example.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.scheduler.notification.provider.MailProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppMailPropertiesTest {
    private static final String API_KEY = "xkeysib-super-secret-key-123";

    @Test
    void shouldMaskApiKeyWhenBrevoToStringIsCalled() {
        var brevo = new AppMailProperties.Brevo("https://api.brevo.com", API_KEY);

        assertThat(brevo.toString())
                .isEqualTo("Brevo[baseUrl=https://api.brevo.com, apiKey=***]")
                .doesNotContain(API_KEY);
    }

    @Test
    void shouldMaskApiKeyWhenWholePropertiesObjectIsLogged() {
        var properties = new AppMailProperties(
                MailProvider.BREVO,
                "no-reply@clinicflow.test",
                "ClinicFlow",
                new AppMailProperties.Brevo("https://api.brevo.com", API_KEY));

        assertThat(properties.toString()).contains("apiKey=***").doesNotContain(API_KEY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldReportBlankAndNoApiKeyWhenKeyIsMissing(String apiKey) {
        var brevo = new AppMailProperties.Brevo("https://api.brevo.com", apiKey);

        assertThat(brevo.hasApiKey()).isFalse();
        assertThat(brevo.toString()).contains("apiKey=<blank>");
    }

    @Test
    void shouldDefaultProviderToBrevoWhenProviderIsNull() {
        var properties = new AppMailProperties(
                null, "no-reply@clinicflow.test", "ClinicFlow", new AppMailProperties.Brevo("https://x", API_KEY));

        assertThat(properties.provider()).isEqualTo(MailProvider.BREVO);
    }

    @Test
    void shouldKeepExplicitProviderWhenProviderIsSet() {
        var properties = new AppMailProperties(
                MailProvider.SMTP,
                "no-reply@clinicflow.test",
                "ClinicFlow",
                new AppMailProperties.Brevo("https://x", ""));

        assertThat(properties.provider()).isEqualTo(MailProvider.SMTP);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void shouldReportNoFromWhenFromIsBlank(String from) {
        var properties = new AppMailProperties(
                MailProvider.BREVO, from, "ClinicFlow", new AppMailProperties.Brevo("https://x", ""));

        assertThat(properties.hasFrom()).isFalse();
    }

    @Test
    void shouldReportFromWhenFromIsPresent() {
        var properties = new AppMailProperties(
                MailProvider.BREVO,
                "no-reply@clinicflow.test",
                "ClinicFlow",
                new AppMailProperties.Brevo("https://x", ""));

        assertThat(properties.hasFrom()).isTrue();
    }

    @Test
    void shouldReportApiKeyWhenKeyIsPresent() {
        assertThat(new AppMailProperties.Brevo("https://api.brevo.com", API_KEY).hasApiKey())
                .isTrue();
    }
}
