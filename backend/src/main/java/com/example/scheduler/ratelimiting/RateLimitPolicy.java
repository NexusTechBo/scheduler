package com.example.scheduler.ratelimiting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record RateLimitPolicy(
        @NotBlank String name,
        List<
                        @Pattern(
                                regexp = "GET|HEAD|POST|PUT|PATCH|DELETE",
                                message = "must be an HTTP method in upper case")
                        String>
                methods,
        @NotEmpty List<@NotBlank String> paths,
        @NotNull RateLimitKeyStrategy key,
        @NotEmpty List<@Valid @NotNull RateLimitBandwidth> limits) {
    public RateLimitPolicy {
        methods = methods == null ? List.of() : List.copyOf(methods);
    }
}
