package com.example.scheduler.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        List<FieldErrorItem> errors
) {
    public record FieldErrorItem(String field, String message) {}

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldErrorItem> errors) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, path, LocalDateTime.now(), errors);
    }
}
