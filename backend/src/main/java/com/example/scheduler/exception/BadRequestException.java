package com.example.scheduler.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String messageKey, Object... args) {
        super(HttpStatus.BAD_REQUEST, messageKey, args);
    }
}
