package com.example.scheduler.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.scheduler.util.LogKeys;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

class ErrorResponseTest {
    @AfterEach
    void tearDown() {
        MDC.remove(LogKeys.TRACE_ID);
    }

    @Test
    void shouldSetCodeAndDetailsWhenBuiltFromCode() {
        var response = ErrorResponse.ofCode(
                HttpStatus.CONFLICT, "appointment.slotTaken", "Ocupado", Map.of("count", 2), "/api/x");

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("Conflict");
        assertThat(response.code()).isEqualTo("appointment.slotTaken");
        assertThat(response.message()).isEqualTo("Ocupado");
        assertThat(response.details()).containsEntry("count", 2);
        assertThat(response.path()).isEqualTo("/api/x");
        assertThat(response.errors()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void shouldLeaveCodeAndDetailsNullWhenBuiltWithoutCode() {
        var response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST, "Invalido", "/api/x", List.of(new FieldErrorItem("startTime", "requerido")));

        assertThat(response.code()).isNull();
        assertThat(response.details()).isNull();
        assertThat(response.errors()).containsExactly(new FieldErrorItem("startTime", "requerido"));
    }

    @Test
    void shouldCopyTraceIdFromMdc() {
        MDC.put(LogKeys.TRACE_ID, "trace-123");

        var response = ErrorResponse.ofCode(HttpStatus.NOT_FOUND, "k", "m", null, "/p");

        assertThat(response.traceId()).isEqualTo("trace-123");
    }

    @Test
    void shouldLeaveTraceIdNullWhenMdcHasNone() {
        var response = ErrorResponse.of(HttpStatus.NOT_FOUND, "m", "/p");

        assertThat(response.traceId()).isNull();
    }
}
