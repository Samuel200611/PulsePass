package com.pulsepass.controller;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-SEC-AUTH-002/010 (seccion 10.1 del PRD de Seguridad).
 *
 * Tabla 7.1: "interpretar JWT manualmente" esta prohibido para
 * Controllers — @AuthenticationPrincipal hace exactamente eso por
 * nosotros: Spring ya resolvio el JWT en JwtAuthenticationFilter y dejo
 * el SecurityPrincipal listo en el SecurityContext; este metodo solo lo
 * recibe.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login -> 200 OK con el JWT, o 401 (via
     * GlobalExceptionHandler) si las credenciales son invalidas o la
     * cuenta esta inactiva (FR-SEC-AUTH-003/004).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * GET /api/auth/me -> 200 OK con los datos minimos del principal.
     * Protegida en SecurityConfig (.authenticated()): si no hay Bearer
     * valido, RestAuthenticationEntryPoint responde 401 antes de llegar
     * aqui.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(
            @AuthenticationPrincipal SecurityPrincipal principal) {
        return ResponseEntity.ok(authService.getAuthenticatedUser(principal));
    }
}
