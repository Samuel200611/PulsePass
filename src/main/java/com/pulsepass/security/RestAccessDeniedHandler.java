package com.pulsepass.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SEC009 / seccion 13 del PRD. Este handler SOLO se invoca para fallos de
 * autorizacion detectados por la propia cadena de Spring Security — es
 * decir, los authorizeHttpRequests(...).hasRole("ADMIN") que fallan ANTES
 * de llegar al Controller (ej. un USER intentando POST /api/events).
 *
 * Los rechazos de PROPIEDAD (ej. "este ticket no es tuyo") los lanza
 * AuthorizationService DESDE DENTRO del Controller, ya con
 * DispatcherServlet en medio: esos SI los captura GlobalExceptionHandler
 * (ver su @ExceptionHandler(AccessDeniedException.class)), nunca este
 * handler. Ambos producen el mismo JSON exacto porque ambos reutilizan el
 * mismo contrato ErrorResponse — ver el ejemplo de la seccion 13 del PRD.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "No tiene permisos para realizar esta operación",
                Map.of());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
