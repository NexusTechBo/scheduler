package com.example.scheduler.security;

import com.example.scheduler.enums.ERole;
import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class SecurityUtils {
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String BEARER_PREFIX = "Bearer ";

    private SecurityUtils() {}

    public static boolean hasRole(Authentication auth, ERole role) {
        var authority = ROLE_PREFIX + role.name();
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    public static boolean isRestrictedToOwnData(Authentication auth) {
        return hasRole(auth, ERole.DOCTOR) && !hasRole(auth, ERole.ADMINISTRATOR);
    }

    public static Long extractUserId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }

    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) return null;
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    public static UsernamePasswordAuthenticationToken authenticated(String subject, Collection<String> roles) {
        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }
}
