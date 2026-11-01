package com.example.scheduler.config;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.ErrorResponseWriter;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.middleware.JwtAuthFilter;
import com.example.scheduler.middleware.RateLimitFilter;
import com.example.scheduler.security.RoleExpressions;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${security.public-paths:}")
    private String[] publicPaths;

    private final ErrorResponseWriter errorResponseWriter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> {
                    if (publicPaths.length > 0) a.requestMatchers(publicPaths).permitAll();
                    a.requestMatchers("/actuator/**").denyAll();
                    a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    a.requestMatchers("/api/guest/**").hasRole(ERole.GUEST.name());
                    a.anyRequest().hasAnyRole(RoleExpressions.STAFF_ROLES.toArray(String[]::new));
                })
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, _) -> errorResponseWriter.write(
                                req, res, HttpStatus.UNAUTHORIZED, MessageKeys.SECURITY_AUTHENTICATION_REQUIRED))
                        .accessDeniedHandler((req, res, _) -> errorResponseWriter.write(
                                req, res, HttpStatus.FORBIDDEN, MessageKeys.SECURITY_ACCESS_DENIED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        var registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }
}
