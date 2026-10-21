package com.example.scheduler.exception;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String messageKey;
    private final Object[] args;
    private final String code;
    private final Integer affectedCount;

    protected ApiException(HttpStatus status, String messageKey, Object... args) {
        this(status, messageKey, null, null, args);
    }

    protected ApiException(HttpStatus status, String messageKey, String code, Integer affectedCount, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args != null ? args : new Object[0];
        this.code = code;
        this.affectedCount = affectedCount;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getCode() {
        return code;
    }

    public Integer getAffectedCount() {
        return affectedCount;
    }
}