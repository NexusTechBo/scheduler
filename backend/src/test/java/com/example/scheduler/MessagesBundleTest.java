package com.example.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesBundleTest {

    private Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            assertThat(input)
                    .withFailMessage("El archivo %s no se encontró en src/main/resources", filename)
                    .isNotNull();
            properties.load(input);
        }
        return properties;
    }

    @Test
    @DisplayName("Debe existir paridad total de claves entre messages.properties y messages_es.properties")
    void testKeysParityBetweenDefaultAndSpanishBundles() throws IOException {
        Properties defaultProps = loadProperties("messages.properties");
        Properties esProps = loadProperties("messages_es.properties");

        Set<String> defaultKeys = defaultProps.stringPropertyNames();
        Set<String> esKeys = esProps.stringPropertyNames();

        assertThat(defaultKeys)
                .withFailMessage("Existen claves en messages.properties que faltan en messages_es.properties: %s",
                        defaultKeys.stream().filter(k -> !esKeys.contains(k)).toList())
                .isEqualTo(esKeys);
    }
}