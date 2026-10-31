package com.example.scheduler.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        @DefaultValue("true")
        boolean enabled,
        @Positive
        long cacheMaxSize,
        List<@NotBlank String> excludedPaths,
        List<@Valid @NotNull Policy> policies
) {
    public RateLimitProperties {
        excludedPaths = excludedPaths == null ? List.of() : List.copyOf(excludedPaths);
        policies = policies == null ? List.of() : List.copyOf(policies);
    }

    @AssertTrue(message = "policy names must be unique")
    public boolean isPolicyNamesUnique() {
        var names = policies.stream().map(Policy::name).filter(Objects::nonNull).toList();
        return names.size() == new HashSet<>(names).size();
    }

    public enum KeyStrategy {
        IP, USER
    }

    public record Policy(
            @NotBlank
            String name,
            List<@Pattern(regexp = "GET|HEAD|POST|PUT|PATCH|DELETE", message = "must be an HTTP method in upper case") String> methods,
            @NotEmpty
            List<@NotBlank String> paths,
            @NotNull
            KeyStrategy key,
            @NotEmpty
            List<@Valid @NotNull Limit> limits
    ) {
        public Policy {
            methods = methods == null ? List.of() : List.copyOf(methods);
        }
    }

    public record Limit(
            @Positive
            long capacity,
            @NotNull
            Duration period
    ) {
        @AssertTrue(message = "must be a positive duration")
        public boolean isPeriodPositive() {
            return period == null || (!period.isNegative() && !period.isZero());
        }
    }
}
