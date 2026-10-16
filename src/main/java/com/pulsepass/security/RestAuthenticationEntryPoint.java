package com.pulsepass.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SEC009 / seccion 13 del PRD: se dispara cuando una ruta protegida recibe
 * un request sin autenticacion valida (sin Bearer, Bearer invalido/vencido
 * ya descartado por JwtAuthenticationFilter, o sin autenticar en absoluto).
 *
 * Importante (seccion 13, nota del PRD): esto ocurre DENTRO de la cadena
 * de filtros de Spring Security, ANTES de que DispatcherServlet despache
 * al Controller — @RestControllerAdvice (GlobalExceptionHandler) NO cubre
 * este caso automaticamente, por eso se escribe el JSON a mano con el
 * MISMO ObjectMapper que usa el resto de la aplicacion, respetando
 * exactamente el contrato de ErrorResponse.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Authentication is required to access this resource.",
                Map.of());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
