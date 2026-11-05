package com.example.scheduler.config;

import com.example.scheduler.notification.provider.MailProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(
        @DefaultValue("brevo") MailProvider provider,
        @DefaultValue("") String from,
        @DefaultValue("Scheduler") String fromName,
        @Valid @NotNull @DefaultValue Brevo brevo) {

    public AppMailProperties {
        provider = provider == null ? MailProvider.DEFAULT : provider;
    }

    public boolean hasFrom() {
        return StringUtils.hasText(from);
    }

    public record Brevo(
            @NotBlank @DefaultValue("https://api.brevo.com") String baseUrl,
            @DefaultValue("") String apiKey) {

        public boolean hasApiKey() {
            return StringUtils.hasText(apiKey);
        }

        @Override
        public @NonNull String toString() {
            return "Brevo[baseUrl=" + baseUrl + ", apiKey=" + (hasApiKey() ? "***" : "<blank>") + "]";
        }
    }
}
