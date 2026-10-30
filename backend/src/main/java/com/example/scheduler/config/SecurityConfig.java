package com.example.scheduler.config;

import com.example.scheduler.exception.ErrorResponse;
import com.example.scheduler.middleware.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${security.public-paths:}")
    private String[] publicPaths;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> {
                    if (publicPaths.length > 0)
                        a.requestMatchers(publicPaths).permitAll();
                    a.requestMatchers("/actuator/**").denyAll();
                    a.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, _) -> writeError(req, res, HttpStatus.UNAUTHORIZED, "security.authenticationRequired"))
                        .accessDeniedHandler((req, res, _) -> writeError(req, res, HttpStatus.FORBIDDEN, "security.accessDenied")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String messageKey) throws IOException {
        var message = messageSource.getMessage(messageKey, null, localeResolver.resolveLocale(request));
        var body = ErrorResponse.of(status, message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
