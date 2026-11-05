package com.example.scheduler.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MessageKeysBundleTest {
    @ParameterizedTest
    @ValueSource(strings = {"messages.properties", "messages_es.properties"})
    void shouldContainEveryMessageKeyWhenBundleIsLoaded(String bundle) throws IOException {
        var properties = load(bundle);

        assertThat(messageKeys())
                .allSatisfy(key ->
                        assertThat(properties).as("%s in %s", key, bundle).containsKey(key));
    }

    private static List<String> messageKeys() {
        return Arrays.stream(MessageKeys.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .map(MessageKeysBundleTest::value)
                .toList();
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Properties load(String bundle) throws IOException {
        var properties = new Properties();
        try (var in = MessageKeysBundleTest.class.getClassLoader().getResourceAsStream(bundle)) {
            assertThat(in).as(bundle).isNotNull();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
