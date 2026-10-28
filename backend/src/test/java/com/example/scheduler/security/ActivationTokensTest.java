package com.example.scheduler.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ActivationTokensTest {

    @Test
    void generatesUrlSafe32ByteTokensWithoutPadding() {
        var token = ActivationTokens.generate();

        assertThat(token).hasSize(43).matches("^[A-Za-z0-9_-]+$");
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(ActivationTokens.generate()).isNotEqualTo(token);
    }

    @Test
    void hashIsLowercaseSha256Hex() {
        assertThat(ActivationTokens.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hashIsDeterministicAndDiffersFromToken() {
        var token = ActivationTokens.generate();

        assertThat(ActivationTokens.hash(token))
                .hasSize(64)
                .isEqualTo(ActivationTokens.hash(token))
                .isNotEqualTo(ActivationTokens.hash(token + "x"))
                .doesNotContain(token);
    }
}
