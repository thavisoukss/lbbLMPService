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
        String msg = messageSource.getMessage("error.MISSING_HEADER.message", new Object[]{e.getHeaderName()}, LocaleContextHolder.getLocale());
        log.warn("Exception handled: status=400 code=MISSING_HEADER header={}", e.getHeaderName());
        return ResponseEntity.badRequest().body(
                Map.of("status", "error",
                       "error", Map.of("code", "MISSING_HEADER"),
                       "message", msg)
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException e) {
        String msg = getMessage(e.getCode(), e.getMessage());
        log.warn("Exception handled: status=404 code={} message={}", e.getCode(), msg);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", msg)
        );
    }

    @ExceptionHandler(MSmartException.class)
    public ResponseEntity<?> handleMSmart(MSmartException e) {
        String msg = getMessage(e.getCode(), e.getMessage());
        log.warn("Exception handled: status=200 code={} message={}", e.getCode(), msg);
        return ResponseEntity.ok(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", msg)
        );
    }

    @ExceptionHandler(SecurityQuestionException.class)
    public ResponseEntity<?> handleSecurityQuestion(SecurityQuestionException e) {
        String msg = getMessage(e.getCode(), e.getMessage());
        log.warn("Exception handled: status=400 code={} message={}", e.getCode(), msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", msg)
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e) {
        String msg = getMessage(e.getCode(), e.getMessage());
        log.warn("Exception handled: status=200 code={} message={}", e.getCode(), msg);
        return ResponseEntity.ok(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", msg)
        );
    }

    private String getMessage(String code, String defaultMessage) {
        if (code == null || code.isBlank()) {
            return defaultMessage;
        }
        if ("RESOURCE_NOT_FOUND".equals(code)) {
            if (defaultMessage == null || defaultMessage.isBlank() || defaultMessage.toLowerCase().contains("resource not found")) {
                return messageSource.getMessage("error.RESOURCE_NOT_FOUND.message", null, "Resource not found", LocaleContextHolder.getLocale());
            }
            return defaultMessage;
        }
        return messageSource.getMessage("error." + code + ".message", null, defaultMessage, LocaleContextHolder.getLocale());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Exception handled: status=400 code=INVALID_REQUEST detail={}", detail);
        return ResponseEntity.badRequest().body(
                Map.of("status", "error",
                       "error", Map.of("code", "INVALID_REQUEST"),
                       "message", detail)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e) {
        log.error("Exception handled: status=500 code=INTERNAL_ERROR", e);
        String msg = messageSource.getMessage("error.INTERNAL_ERROR.message", null, LocaleContextHolder.getLocale());
        return ResponseEntity.internalServerError().body(
                Map.of("status", "error",
                       "error", Map.of("code", "INTERNAL_ERROR"),
                       "message", msg)
        );
    }
}
