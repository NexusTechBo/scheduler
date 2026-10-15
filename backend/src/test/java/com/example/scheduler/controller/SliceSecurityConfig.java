package com.example.scheduler.controller;

import com.example.scheduler.config.SecurityConfig;
import com.example.scheduler.middleware.JwtAuthFilter;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({SecurityConfig.class, JwtUtil.class, ActiveUserChecker.class})
public class SliceSecurityConfig {
    public static final String SECRET = "jwt.secret=test-secret-test-secret-test-secret-0123456789";
    public static final String PUBLIC_PATHS = "security.public-paths=/api/auth/login";

    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
