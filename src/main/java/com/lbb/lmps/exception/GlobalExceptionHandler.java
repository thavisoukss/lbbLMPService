package com.lbb.lmps.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("Missing required header: {}", e.getHeaderName());
        return ResponseEntity.badRequest().body(
                Map.of("status", "error",
                       "error", Map.of("code", "MISSING_HEADER"),
                       "message", "Required header '" + e.getHeaderName() + "' is not present")
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("status", "error",
                       "error", Map.of("code", "RESOURCE_NOT_FOUND"),
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
}
