package com.example.scheduler.exception;

import com.example.scheduler.util.LogKeys;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorItem> errors,
        String traceId
) {
    public record FieldErrorItem(String field, String message) {}

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldErrorItem> errors) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                errors,
                MDC.get(LogKeys.TRACE_ID)
        );
    }
}
