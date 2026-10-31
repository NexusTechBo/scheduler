package com.example.scheduler.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String messageKey, Object... args) {
        super(HttpStatus.FORBIDDEN, messageKey, args);
    }
}
