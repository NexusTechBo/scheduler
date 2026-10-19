package com.example.scheduler.middleware;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ActiveUserChecker activeUserChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        try {
            var header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                jwtUtil.parse(header.substring(7)).ifPresent(claims -> {
                    TenantContext.setCurrentTenant("clinic_" + claims.get("clinicId", Long.class));
                    var role = claims.get("role", String.class);
                    var userId = Long.valueOf(claims.getSubject());
                    try {
                        if (!activeUserChecker.isActive(userId, role)) return;
                    } catch (DataAccessException e) {
                        log.warn("Active user lookup failed for user {}: {}", userId, e.getMessage());
                        return;
                    }
                    List<SimpleGrantedAuthority> authorities = role != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            : List.of();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
                });
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
