package com.example.scheduler.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {

    public static String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> Objects.requireNonNull(a.getAuthority()).replace("ROLE_", ""))
                .findFirst()
                .orElse(null);
    }
}
