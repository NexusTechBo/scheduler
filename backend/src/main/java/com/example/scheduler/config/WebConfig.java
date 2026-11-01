package com.example.scheduler.config;

import com.example.scheduler.middleware.RateLimitFilter;
import com.example.scheduler.util.LogKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(LogKeys.REQUEST_ID_HEADER, HttpHeaders.RETRY_AFTER, RateLimitFilter.REMAINING_HEADER)
                .allowCredentials(false)
                .maxAge(3600);
    }
}
