package com.pulsepass.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tabla 7.1 del PRD: "leer Bearer, validar JWT, construir contexto
 * seguro"; prohibido crear cuentas o persistir tokens de acceso (esto
 * ultimo tampoco existe en el MVP: sin denylist, seccion 12.3).
 *
 * OncePerRequestFilter garantiza una sola ejecucion por request incluso
 * con forwards/includes internos de Servlet.
 *
 * Regla critica de la seccion 14.3, advertencia #5: "debe dejar continuar
 * requests sin header solo para que la cadena decida si la ruta es
 * publica; si se suministra Bearer invalido, jamas autenticarlo por
 * accidente." Por eso este filtro NUNCA devuelve un error el mismo: ante
 * cualquier problema (sin header, header invalido, token expirado,
 * usuario inactivo) simplemente NO establece autenticacion y deja
 * continuar la cadena — authorizeHttpRequests + RestAuthenticationEntryPoint
 * se encargan de convertir "sin autenticacion en ruta protegida" en 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        // FR-SEC-AUTH-007: el token SOLO se lee del header Authorization.
        // request.getParameter(...) jamas se consulta para esto, de modo
        // que un JWT en query string simplemente no tiene efecto.
        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateIfValid(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isBlank() ? null : token;
        }

        return null;
    }

    private void authenticateIfValid(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.parseAndValidate(token);
            Long userId = Long.valueOf(claims.getSubject());

            // SEC011: se recarga el usuario desde la base de datos en
            // CADA request — nunca se confia en roles/estado "cacheados"
            // dentro del propio JWT.
            UserDetails principal = userDetailsService.loadUserById(userId);

            if (!principal.isEnabled()) {
                // Usuario desactivado despues de emitido el token: no se
                // autentica. La ruta protegida terminara en 401 via
                // RestAuthenticationEntryPoint, sin revelar el motivo.
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            // JwtException cubre ExpiredJwtException, MalformedJwtException,
            // SignatureException, UnsupportedJwtException (FR-SEC-AUTH-006).
            // IllegalArgumentException cubre un "sub" no numerico o un
            // token vacio. UsernameNotFoundException cubre un userId que
            // ya no existe. En los tres casos: no autenticar, no filtrar
            // detalles, dejar que el resto de la cadena decida.
            SecurityContextHolder.clearContext();
        }
    }
}
