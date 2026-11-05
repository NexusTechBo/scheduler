package com.example.scheduler.controller;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.jwt.JwtClaims;
import com.example.scheduler.security.jwt.JwtUtil;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class SliceTokens {
    static final String ADMIN_TOKEN = "admin-token";
    static final String ASSISTANT_TOKEN = "assistant-token";
    static final String GUEST_TOKEN = "guest-token";
    static final long ADMIN_ID = 1L;
    static final long ASSISTANT_ID = 3L;

    private SliceTokens() {}

    static void givenStandardTokens(JwtUtil jwtUtil, ActiveUserChecker activeUserChecker) {
        givenToken(jwtUtil, ADMIN_TOKEN, ADMIN_ID, "ADMINISTRATOR", List.of("ADMINISTRATOR"));
        givenToken(jwtUtil, ASSISTANT_TOKEN, ASSISTANT_ID, "ASSISTANT", List.of("ASSISTANT"));
        givenToken(jwtUtil, GUEST_TOKEN, 0L, "GUEST", List.of());
        when(activeUserChecker.isActive(anyLong(), anyString(), anyCollection()))
                .thenReturn(true);
    }

    static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static void givenToken(JwtUtil jwtUtil, String token, long userId, String role, List<String> roles) {
        var builder = Jwts.claims()
                .subject(Long.toString(userId))
                .add(JwtClaims.ROLE, role)
                .add(JwtClaims.CLINIC_ID, 7L)
                .add(JwtClaims.CLINIC_SLUG, "demo");
        if (!roles.isEmpty()) builder.add(JwtClaims.ROLES, roles);
        when(jwtUtil.parse(token)).thenReturn(Optional.of(builder.build()));
    }
}
