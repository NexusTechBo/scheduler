package com.example.scheduler.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String messageKey;
    private final Object[] args;

    protected ApiException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args != null ? args : new Object[0];
    }
}
