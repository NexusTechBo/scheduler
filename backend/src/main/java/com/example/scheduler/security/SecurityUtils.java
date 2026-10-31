package com.example.scheduler.security;

import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class SecurityUtils {
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String BEARER_PREFIX = "Bearer ";

    private SecurityUtils() {}

    public static String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> stripRolePrefix(Objects.requireNonNull(a.getAuthority())))
                .findFirst()
                .orElse(null);
    }

    private static String stripRolePrefix(String authority) {
        return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
    }

    public static Long extractUserId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }

    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) return null;
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    public static UsernamePasswordAuthenticationToken authenticated(String subject, String role) {
        var authorities = List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role));
        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }
}
