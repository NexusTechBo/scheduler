package com.example.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

// Authentication is JWT-only (JwtAuthFilter); without this exclusion Boot creates an in-memory user and logs its password.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SchedulerApplication {

    static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}
