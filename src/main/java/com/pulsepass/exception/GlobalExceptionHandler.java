package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * CTRL-007 / CTRL-008: unico punto de traduccion entre excepciones de
 * dominio (o de Spring MVC) y respuestas HTTP (seccion 14/15 del PRD de
 * Controllers). Ningun Controller implementa try/catch propio: todo pasa
 * por aqui, y todos los handlers retornan ResponseEntity<ErrorResponse>.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =====================================================
    // 400 — Bean Validation (CTRL-005, AC-CTRL-004)
    // Spring MVC lanza esta excepcion cuando @Valid falla en un
    // @RequestBody; se captura antes de que la ejecucion llegue al Service.
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
    // 400 — JSON mal formado (CTRL-005)
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
    // 500 — Error inesperado (NFR-CTRL-007: sin stack traces ni
    // detalles internos; mensaje generico fijo)
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
