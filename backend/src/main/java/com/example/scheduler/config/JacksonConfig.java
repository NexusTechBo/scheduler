package com.example.scheduler.config;

import com.example.scheduler.config.stripping.StrippingStringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {
    @Bean
    public JacksonModule stringStripModule() {
        return new SimpleModule("string-strip").addDeserializer(String.class, StrippingStringDeserializer.STRIP);
    }
}
