package com.example.scheduler.exception;

import com.example.scheduler.util.LogKeys;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

/**
 * Error contract consumed by the frontend. {@code code} is the message key of an {@link ApiException} and {@code
 * details} its optional structured data; both are omitted for other errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, Object> details,
        String path,
        List<FieldErrorItem> errors,
        String traceId) {
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldErrorItem> errors) {
        return create(status, null, message, null, path, errors);
    }

    public static ErrorResponse ofCode(
            HttpStatus status, String code, String message, Map<String, Object> details, String path) {
        return create(status, code, message, details, path, null);
    }

    private static ErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details,
            String path,
            List<FieldErrorItem> errors) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                details,
                path,
                errors,
                MDC.get(LogKeys.TRACE_ID));
    }
}
