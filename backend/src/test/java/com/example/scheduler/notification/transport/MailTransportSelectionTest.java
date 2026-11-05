package com.example.scheduler.notification.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.scheduler.config.AppMailProperties;
import com.example.scheduler.config.BrevoExchangeConfig;
import com.example.scheduler.exchange.BrevoExchange;
import com.example.scheduler.notification.provider.ConditionalOnMailProvider;
import com.example.scheduler.notification.provider.MailProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

class MailTransportSelectionTest {
    private static final String API_KEY = "xkeysib-super-secret-key-123";

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppMailProperties.class)
    static class MailPropertiesConfig {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    MailPropertiesConfig.class,
                    BrevoExchangeConfig.class,
                    BrevoMailTransport.class,
                    SmtpMailTransport.class)
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withPropertyValues("app.mail.from=no-reply@clinicflow.test", "app.mail.brevo.api-key=" + API_KEY);

    @Test
    void shouldRegisterOnlyBrevoTransportWhenProviderIsMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MailTransport.class);
            assertThat(context).hasSingleBean(BrevoMailTransport.class);
            assertThat(context).hasSingleBean(BrevoExchange.class);
            assertThat(context).doesNotHaveBean(SmtpMailTransport.class);
            assertThat(context.getBean(AppMailProperties.class).provider()).isEqualTo(MailProvider.BREVO);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("A blank MAIL_PROVIDER falls back to Brevo for both the condition and the bound properties")
    void shouldRegisterOnlyBrevoTransportWhenProviderIsBlank(String provider) {
        runner.withPropertyValues("app.mail.provider=" + provider).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MailTransport.class);
            assertThat(context).hasSingleBean(BrevoMailTransport.class);
            assertThat(context.getBean(AppMailProperties.class).provider()).isEqualTo(MailProvider.BREVO);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"brevo", "BREVO", "BrEvO", " brevo "})
    void shouldRegisterOnlyBrevoTransportWhenProviderIsBrevoInAnyCase(String provider) {
        runner.withPropertyValues("app.mail.provider=" + provider).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MailTransport.class);
            assertThat(context).hasSingleBean(BrevoMailTransport.class);
            assertThat(context).doesNotHaveBean(SmtpMailTransport.class);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"smtp", "SMTP", "Smtp"})
    void shouldRegisterOnlySmtpTransportWhenProviderIsSmtp(String provider) {
        runner.withPropertyValues("app.mail.provider=" + provider).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MailTransport.class);
            assertThat(context).hasSingleBean(SmtpMailTransport.class);
            assertThat(context).doesNotHaveBean(BrevoMailTransport.class);
            assertThat(context).doesNotHaveBean(BrevoExchange.class);
            assertThat(context.getBean(AppMailProperties.class).provider()).isEqualTo(MailProvider.SMTP);
        });
    }

    @Test
    void shouldStartWithoutApiKeyWhenProviderIsBrevo() {
        new ApplicationContextRunner()
                .withUserConfiguration(MailPropertiesConfig.class, BrevoExchangeConfig.class, BrevoMailTransport.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(BrevoMailTransport.class);
                    assertThat(context.getBean(AppMailProperties.class).brevo().hasApiKey())
                            .isFalse();
                });
    }

    @Nested
    class UnknownProvider {
        @Test
        void shouldRegisterNoTransportWhenProviderIsUnknown() {
            // Condition only: the properties are supplied as a bean so that binding does not run.
            new ApplicationContextRunner()
                    .withUserConfiguration(BrevoExchangeConfig.class, BrevoMailTransport.class, SmtpMailTransport.class)
                    .withBean(AppMailProperties.class, () -> mock(AppMailProperties.class))
                    .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                    .withPropertyValues("app.mail.provider=sendgrid")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean(MailTransport.class);
                        assertThat(context).doesNotHaveBean(BrevoExchange.class);
                    });
        }

        @Test
        void shouldFailStartupWithBindingErrorWhenProviderIsUnknown() {
            runner.withPropertyValues("app.mail.provider=sendgrid").run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("sendgrid");
                assertThat(context.getStartupFailure()).hasStackTraceContaining("app.mail.provider");
            });
        }
    }
}
