package com.lbb.lmps.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.i18n.LocaleContextHolder;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("Missing required header: {}", e.getHeaderName());
        String msg = messageSource.getMessage("error.MISSING_HEADER.message", new Object[]{e.getHeaderName()}, LocaleContextHolder.getLocale());
        return ResponseEntity.badRequest().body(
                Map.of("status", "error",
                       "error", Map.of("code", "MISSING_HEADER"),
                       "message", msg)
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", e.getMessage())
        );
    }

    @ExceptionHandler(MSmartException.class)
    public ResponseEntity<?> handleMSmart(MSmartException e) {
        log.warn("m-smart business error: {}", e.getMessage());
        return ResponseEntity.ok(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", e.getMessage())
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e) {
        log.warn("business error: code={} message={}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", e.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", detail);
        return ResponseEntity.badRequest().body(
                Map.of("status", "error",
                       "error", Map.of("code", "INVALID_REQUEST"),
                       "message", detail)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        String msg = messageSource.getMessage("error.INTERNAL_ERROR.message", null, LocaleContextHolder.getLocale());
        return ResponseEntity.internalServerError().body(
                Map.of("status", "error",
                       "error", Map.of("code", "INTERNAL_ERROR"),
                       "message", msg)
        );
    }
}
