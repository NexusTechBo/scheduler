package com.example.scheduler.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String messageKey;
    private final Object[] args;
    private final Map<String, Object> details;

    protected ApiException(HttpStatus status, String messageKey, Object... args) {
        this(status, null, messageKey, args);
    }

    protected ApiException(HttpStatus status, Map<String, Object> details, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args != null ? args : new Object[0];
        this.details =
                details == null || details.isEmpty() ? null : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }
}
