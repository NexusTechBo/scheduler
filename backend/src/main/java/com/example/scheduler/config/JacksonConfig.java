package com.example.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

import java.util.Locale;

@Configuration
public class JacksonConfig {

    @Bean
    public JacksonModule stringStripModule() {
        return new SimpleModule("string-strip").addDeserializer(String.class, StrippingStringDeserializer.STRIP);
    }

    static final class StrippingStringDeserializer extends ValueDeserializer<String> {
        static final StrippingStringDeserializer STRIP = new StrippingStringDeserializer(true);
        static final StrippingStringDeserializer KEEP = new StrippingStringDeserializer(false);

        private final boolean strip;

        private StrippingStringDeserializer(boolean strip) {
            this.strip = strip;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            var value = StringDeserializer.instance.deserialize(p, ctxt);
            return strip && value != null ? value.strip() : value;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            var isPassword = property != null && property.getName().toLowerCase(Locale.ROOT).contains("password");
            return isPassword ? KEEP : STRIP;
        }
    }
}
