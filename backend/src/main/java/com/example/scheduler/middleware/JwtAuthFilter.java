package com.example.scheduler.middleware;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.SecurityUtils;
import com.example.scheduler.security.jwt.JwtClaims;
import com.example.scheduler.security.jwt.JwtUtil;
import com.example.scheduler.tenant.TenantContext;
import com.example.scheduler.util.LogKeys;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final ActiveUserChecker activeUserChecker;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        try {
            var token = SecurityUtils.extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
            if (token != null) {
                jwtUtil.parse(token).ifPresent(claims -> {
                    try {
                        authenticate(request, claims);
                    } catch (RuntimeException e) {
                        log.warn("Token claims rejected: {}", e.getClass().getSimpleName());
                        SecurityContextHolder.clearContext();
                        TenantContext.clear();
                    }
                });
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove(LogKeys.USER_ID);
            MDC.remove(LogKeys.ROLE);
        }
    }

    private void authenticate(HttpServletRequest request, Claims claims) {
        var clinicId = claims.get(JwtClaims.CLINIC_ID, Long.class);
        TenantContext.setCurrentTenant(clinicId, claims.get(JwtClaims.CLINIC_SLUG, String.class));
        var role = claims.get(JwtClaims.ROLE, String.class);
        var userId = Long.valueOf(claims.getSubject());
        try {
            if (!activeUserChecker.isActive(userId, role)) {
                log.warn(
                        "Token rejected: user {} with role {} in clinic {} is inactive or its role changed",
                        userId,
                        role,
                        clinicId);
                return;
            }
        } catch (DataAccessException e) {
            log.warn(
                    "Active user lookup failed for user {}: {}",
                    userId,
                    e.getClass().getSimpleName());
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(SecurityUtils.authenticated(claims.getSubject(), role));
        bindLogContext(
                request, clinicId, ERole.GUEST.name().equals(role) ? LogKeys.GUEST_USER : claims.getSubject(), role);
    }

    private static void bindLogContext(HttpServletRequest request, Long clinicId, String userId, String role) {
        MDC.put(LogKeys.USER_ID, userId);
        MDC.put(LogKeys.ROLE, role);
        if (clinicId != null) request.setAttribute(LogKeys.ATTR_CLINIC_ID, clinicId.toString());
        request.setAttribute(LogKeys.ATTR_USER_ID, userId);
        request.setAttribute(LogKeys.ATTR_ROLE, role);
    }
}
