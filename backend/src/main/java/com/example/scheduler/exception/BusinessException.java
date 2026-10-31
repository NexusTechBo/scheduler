package com.example.scheduler.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {
    public BusinessException(String messageKey, Object... args) {
        super(HttpStatus.CONFLICT, messageKey, args);
    }
}
