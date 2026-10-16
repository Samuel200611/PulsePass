package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * FR-SEC-AUTH-002 (seccion 10.1 del PRD de Seguridad).
 *
 * identifier acepta username O email indistintamente — AuthServiceImpl
 * resuelve cual es mediante UserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase.
 * No se valida formato aqui (no sabemos si es un username o un email),
 * solo que no venga vacio.
 */
public record LoginRequest(

        @NotBlank(message = "Identifier is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password

) {
}
