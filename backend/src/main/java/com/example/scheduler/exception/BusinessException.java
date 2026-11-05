package com.example.scheduler.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {
    public BusinessException(String messageKey, Object... args) {
        super(HttpStatus.CONFLICT, messageKey, args);
    }

    private BusinessException(Map<String, Object> details, String messageKey, Object... args) {
        super(HttpStatus.CONFLICT, details, messageKey, args);
    }

    public static BusinessException withDetails(Map<String, Object> details, String messageKey, Object... args) {
        return new BusinessException(details, messageKey, args);
    }
}
