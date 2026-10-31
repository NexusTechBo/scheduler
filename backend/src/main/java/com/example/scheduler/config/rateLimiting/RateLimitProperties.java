package com.example.scheduler.config.rateLimiting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @Positive long cacheMaxSize,
        List<@NotBlank String> excludedPaths,
        List<@Valid @NotNull RateLimitPolicy> policies) {
    public RateLimitProperties {
        excludedPaths = excludedPaths == null ? List.of() : List.copyOf(excludedPaths);
        policies = policies == null ? List.of() : List.copyOf(policies);
    }
}
