package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    // =====================================================
    // 400 — Bean Validation
    // =====================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Validation failed", details);
    }

    // =====================================================
    // 400 — JSON mal formado
    // =====================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex) {

        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Malformed JSON request", Map.of());
    }

    // =====================================================
    // 404 — Recurso inexistente
    // =====================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return buildResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), Map.of());
    }

    // =====================================================
    // 401 — Credenciales invalidas
    // =====================================================
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized",
                ex.getMessage(), Map.of());
    }

    // =====================================================
    // 403 — Rechazos de PROPIEDAD
    // =====================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {

        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden",
                "No tiene permisos para realizar esta operación", Map.of());
    }

    // =====================================================
    // 409 — Conflicto de unicidad
    // =====================================================

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex) {

        return buildResponse(HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), Map.of());
    }

    // =====================================================
    // 409 — Regla de negocio incumplida
    // =====================================================

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex) {

        return buildResponse(HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), Map.of());
    }

    // =====================================================
    // 500 — Error inesperado
    // =====================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", "An unexpected error occurred.", Map.of());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                         String error,
                                                         String message,
                                                         Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(), status.value(), error, message, details);

        return ResponseEntity.status(status).body(body);
    }
}
