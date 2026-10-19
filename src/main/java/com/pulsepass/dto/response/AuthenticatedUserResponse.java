package com.pulsepass.dto.response;

import java.util.List;

/**
 * FR-SEC-AUTH-010 (seccion 10.1 del PRD de Seguridad).
 * "informacion minima del principal, no secretos": sin passwordHash, sin
 * UserProfile completo — solo lo que identifica al principal ante el
 * propio cliente que ya tiene su token.
 */
public record AuthenticatedUserResponse(

        Long id,

        String username,

        String email,

        List<String> roles

) {
}
