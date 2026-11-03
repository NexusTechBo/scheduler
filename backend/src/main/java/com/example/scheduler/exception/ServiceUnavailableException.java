package com.example.scheduler.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String messageKey, Object... args) {
        super(HttpStatus.SERVICE_UNAVAILABLE, messageKey, args);
    }
}
