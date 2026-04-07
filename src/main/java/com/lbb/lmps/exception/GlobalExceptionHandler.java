package com.lbb.lmps.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MSmartException.class)
    public ResponseEntity<?> handleMSmart(MSmartException e) {
        log.warn("m-smart business error: {}", e.getMessage());
        return ResponseEntity.ok(
                Map.of("status", "error",
                       "error", Map.of("code", e.getCode()),
                       "message", e.getMessage())
        );
    }
}
