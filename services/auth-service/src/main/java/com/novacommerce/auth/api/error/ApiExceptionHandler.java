package com.novacommerce.auth.api.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<ApiError> auth(AuthException exception, HttpServletRequest request) {
        return response(exception.getStatus().value(), exception.getCode(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        return response(400, "VALIDATION_FAILED", "Request validation failed", request, fields);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        return response(409, "EMAIL_ALREADY_REGISTERED", "Email is already registered", request, Map.of());
    }

    private ResponseEntity<ApiError> response(int status, String code, String message, HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status, code, message, request.getRequestURI(), fields));
    }
}
