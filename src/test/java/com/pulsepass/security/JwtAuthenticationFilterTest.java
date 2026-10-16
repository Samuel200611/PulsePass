package com.pulsepass.security;

import com.pulsepass.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Seccion 17.1 del PRD: "sin Bearer, Bearer valido, Bearer invalido, doble
 * header sospechoso, no sobrescribir autenticacion establecida de forma
 * insegura."
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private SecurityPrincipal activePrincipal() {
        return new SecurityPrincipal(1L, "andrea", "andrea@email.com",
                "$2a$10$hashed", true, Set.of(Role.USER));
    }

    // ---------------------------------------------------------
    // Sin header Authorization
    // ---------------------------------------------------------

    @Test
    void shouldContinueChainWithoutAuthenticatingWhenNoHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).parseAndValidate(any());
    }

    @Test
    void shouldContinueChainWhenHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).parseAndValidate(any());
    }

    // ---------------------------------------------------------
    // Bearer valido
    // ---------------------------------------------------------

    @Test
    void shouldAuthenticateWhenBearerTokenIsValid() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtService.parseAndValidate("valid.jwt.token")).thenReturn(claims);
        when(userDetailsService.loadUserById(1L)).thenReturn(activePrincipal());

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(SecurityPrincipal.class);
        assertThat(((SecurityPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    // ---------------------------------------------------------
    // Bearer invalido (expirado, manipulado, etc.) — FR-SEC-AUTH-006
    // ---------------------------------------------------------

    @Test
    void shouldNotAuthenticateWhenTokenIsExpired() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.jwt.token");
        when(jwtService.parseAndValidate("expired.jwt.token"))
                .thenThrow(mock(ExpiredJwtException.class));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // La cadena SI continua (sera la ruta/entry point quien decida
        // 401 o 200 segun si la ruta es publica).
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenSubjectIsNotNumeric() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("not-a-number");

        when(request.getHeader("Authorization")).thenReturn("Bearer weird.jwt.token");
        when(jwtService.parseAndValidate("weird.jwt.token")).thenReturn(claims);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ---------------------------------------------------------
    // SEC011 — usuario desactivado despues de emitido el token
    // ---------------------------------------------------------

    @Test
    void shouldNotAuthenticateWhenUserIsNoLongerEnabled() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");

        SecurityPrincipal disabledPrincipal = new SecurityPrincipal(
                1L, "andrea", "andrea@email.com", "$2a$10$hashed", false, Set.of(Role.USER));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtService.parseAndValidate("valid.jwt.token")).thenReturn(claims);
        when(userDetailsService.loadUserById(1L)).thenReturn(disabledPrincipal);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ---------------------------------------------------------
    // Doble header sospechoso
    // ---------------------------------------------------------

    @Test
    void shouldOnlyConsultTheFirstAuthorizationHeaderValue() throws Exception {
        // request.getHeader(...) — a diferencia de getHeaders(...), que
        // devuelve un Enumeration con TODOS los valores — ya resuelve a
        // un unico valor segun el propio Servlet API (el primero
        // recibido). El filtro usa getHeader deliberadamente, nunca
        // getHeaders, asi que estructuralmente no puede "elegir" entre
        // dos valores sospechosos: solo ve el que el contenedor le
        // entrega.
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");

        when(request.getHeader("Authorization")).thenReturn("Bearer first.jwt.token");
        when(jwtService.parseAndValidate("first.jwt.token")).thenReturn(claims);
        when(userDetailsService.loadUserById(1L)).thenReturn(activePrincipal());

        filter.doFilter(request, response, filterChain);

        verify(jwtService).parseAndValidate("first.jwt.token");
        verify(jwtService, never()).parseAndValidate(org.mockito.ArgumentMatchers.argThat(
                token -> !"first.jwt.token".equals(token)));
    }

    // ---------------------------------------------------------
    // No sobrescribir autenticacion ya establecida
    // ---------------------------------------------------------

    @Test
    void shouldNotOverwriteExistingAuthentication() throws Exception {
        SecurityPrincipal alreadyAuthenticated = new SecurityPrincipal(
                2L, "carlos", "carlos@email.com", "$2a$10$hashed", true, Set.of(Role.USER));
        var existingAuthentication = new UsernamePasswordAuthenticationToken(
                alreadyAuthenticated, null, alreadyAuthenticated.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        when(request.getHeader("Authorization")).thenReturn("Bearer another.jwt.token");

        filter.doFilter(request, response, filterChain);

        // Sigue siendo Carlos: el filtro nunca intento reemplazar una
        // autenticacion ya presente en el contexto.
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(((SecurityPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(2L);
        verify(jwtService, never()).parseAndValidate(any());
    }
}
