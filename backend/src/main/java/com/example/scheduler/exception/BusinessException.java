package com.example.scheduler.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {
    public BusinessException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
