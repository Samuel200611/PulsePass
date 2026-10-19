package com.pulsepass.dto.response;

/**
 * FR-SEC-AUTH-002 (seccion 10.1/10.2 del PRD de Seguridad).
 * expiresIn en SEGUNDOS (900 = 15 minutos), no en milisegundos.
 */
public record AuthResponse(

        String accessToken,

        String tokenType,

        long expiresIn

) {
}
