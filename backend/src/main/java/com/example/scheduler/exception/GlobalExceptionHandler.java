package com.example.scheduler.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.SemanticException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    private String msg(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, key, locale);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        log.warn(
                "Business error {} {} on {} {}",
                ex.getStatus().value(),
                ex.getMessageKey(),
                req.getMethod(),
                req.getRequestURI());
        String resolvedMessage = msg(ex.getMessageKey(), ex.getArgs());
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), resolvedMessage, req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorItem(e.getField(), e.getDefaultMessage()))
                .toList();
        return validationFailed(errors, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(
            HandlerMethodValidationException ex, HttpServletRequest req) {
        var errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new FieldErrorItem(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return validationFailed(errors, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        var errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorItem(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return validationFailed(errors, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, msg(MessageKeys.ERROR_HTTP_MALFORMED_BODY), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, msg(MessageKeys.ERROR_PARAM_INVALID_TYPE, ex.getName()), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, msg(MessageKeys.ERROR_PARAM_MISSING, ex.getParameterName()), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, msg(MessageKeys.ERROR_HTTP_RESOURCE_NOT_FOUND), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(
                HttpStatus.METHOD_NOT_ALLOWED, msg(MessageKeys.ERROR_HTTP_METHOD_NOT_SUPPORTED, ex.getMethod()), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, msg(MessageKeys.ERROR_HTTP_MEDIA_TYPE_NOT_SUPPORTED), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {}: constraint {}", req.getRequestURI(), constraintName(ex));
        return build(HttpStatus.CONFLICT, msg(MessageKeys.ERROR_DATA_CONFLICT), req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException ex, HttpServletRequest req) {
        log.warn("Optimistic locking conflict on {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, msg(MessageKeys.ERROR_DATA_OPTIMISTIC_LOCK), req);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(
            PropertyReferenceException ex, HttpServletRequest req) {
        return build(
                HttpStatus.BAD_REQUEST, msg(MessageKeys.ERROR_PARAM_INVALID_SORT_PROPERTY, ex.getPropertyName()), req);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidApiUsage(
            InvalidDataAccessApiUsageException ex, HttpServletRequest req) {
        if (ex.contains(SemanticException.class)) {
            return build(HttpStatus.BAD_REQUEST, msg(MessageKeys.ERROR_PARAM_INVALID_SORT), req);
        }
        return handleUnexpected(ex, req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, msg(MessageKeys.SECURITY_ACCESS_DENIED), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, msg(MessageKeys.ERROR_SERVER_INTERNAL), req);
    }

    private ResponseEntity<ErrorResponse> validationFailed(List<FieldErrorItem> errors, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST, msg(MessageKeys.ERROR_VALIDATION_FAILED), req.getRequestURI(), errors));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, req.getRequestURI()));
    }

    private static String constraintName(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve
                        && cve.getConstraintName() != null
                ? cve.getConstraintName()
                : ex.getClass().getSimpleName();
    }
}
