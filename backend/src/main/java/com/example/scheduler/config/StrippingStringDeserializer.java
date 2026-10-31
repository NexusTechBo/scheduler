package com.example.scheduler.config;

import java.util.Locale;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.jdk.StringDeserializer;

final class StrippingStringDeserializer extends ValueDeserializer<String> {
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
        var isPassword =
                property != null && property.getName().toLowerCase(Locale.ROOT).contains("password");
        return isPassword ? KEEP : STRIP;
    }
}
