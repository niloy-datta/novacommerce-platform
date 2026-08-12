package com.novacommerce.catalog.api.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CatalogExceptionHandler {
    @ExceptionHandler(CatalogException.class)
    ResponseEntity<CatalogApiError> catalog(CatalogException ex, HttpServletRequest req) { return response(ex.getStatus().value(), ex.getCode(), ex.getMessage(), req, Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<CatalogApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) { Map<String, String> fields = new LinkedHashMap<>(); ex.getBindingResult().getAllErrors().forEach(e -> { if (e instanceof FieldError f) fields.put(f.getField(), f.getDefaultMessage()); }); return response(400, "VALIDATION_FAILED", "Request validation failed", req, fields); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<CatalogApiError> concurrent(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) { return response(409, "CONCURRENT_MODIFICATION", "The catalog record was changed by another request", req, Map.of()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<CatalogApiError> integrity(DataIntegrityViolationException ex, HttpServletRequest req) { return response(409, "DUPLICATE_CATALOG_VALUE", "A catalog value already exists", req, Map.of()); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<CatalogApiError> illegal(IllegalArgumentException ex, HttpServletRequest req) { return response(400, "VALIDATION_FAILED", ex.getMessage(), req, Map.of()); }
    private ResponseEntity<CatalogApiError> response(int status, String code, String message, HttpServletRequest req, Map<String, String> fields) { return ResponseEntity.status(status).body(new CatalogApiError(Instant.now(), status, code, message, req.getRequestURI(), fields)); }
}
