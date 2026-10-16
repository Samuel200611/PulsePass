package com.pulsepass.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Tabla 7.1 del PRD: "declarar filtro, autenticacion y reglas de rutas";
 * prohibido contener negocio de compra/publicacion — todo eso sigue
 * exclusivamente en los Services de la Fase 2, sin tocar.
 *
 * @EnableMethodSecurity habilita @PreAuthorize/@PostAuthorize por si una
 * evolucion futura decide mover alguna regla de aqui a nivel de metodo
 * (la "frontera de negocio segura" que menciona la seccion 3.2 del PRD);
 * el MVP actual resuelve ownership con AuthorizationService invocado
 * explicitamente desde los Controllers, no con anotaciones, pero dejar la
 * puerta abierta no tiene costo.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // SEC003: BCrypt, nunca texto plano.
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Boot conecta automaticamente DaoAuthenticationProvider a
     * CustomUserDetailsService + este PasswordEncoder porque son los
     * unicos beans de esos tipos en el contexto; aqui solo exponemos el
     * AuthenticationManager resultante para que AuthServiceImpl pueda
     * invocar authenticationManager.authenticate(...) durante el login
     * (seccion 5.1 del PRD).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * SEC001/SEC002: denegar por defecto, exponer publicamente solo lo
     * aprobado. El orden de los matchers importa (advertencia #4 de la
     * seccion 14.3 del PRD): rutas literales especificas ANTES que
     * patrones con comodin de un segmento ("*"), porque Spring Security
     * evalua de arriba hacia abajo y usa la PRIMERA regla que calce.
     *
     * Las celdas de la matriz del PRD (seccion 11) que dependen del
     * ESTADO del recurso y no solo de la URL — GET /api/events/{eventCode}
     * (publico solo si PUBLISHED) y GET /api/events/by-artist (idem,
     * filtrado por evento) — se dejan en permitAll() aqui a proposito: la
     * visibilidad real de borradores se decide en EventController con
     * AuthorizationService.canViewEvent(...), porque esa decision necesita
     * leer el Event ya cargado, algo que un matcher de URL no puede hacer
     * (ver advertencia #1 de la seccion 14.3 y la decision abierta de la
     * seccion 20.1 del PRD, que deja esto explicitamente a criterio de
     * implementacion).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                // Solo API Bearer, sin cookies de autenticacion (advertencia
                // #6 de la seccion 14.3: si eso cambia, revisar CSRF).
                .csrf(AbstractHttpConfigurer::disable)
                // FR-SEC-AUTH-008: la API no usa JSESSIONID como autenticacion.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // -----------------------------------------------
                        // Autenticacion (2 endpoints nuevos)
                        // -----------------------------------------------
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // -----------------------------------------------
                        // FR-SEC-USR-01: registro publico (rol inicial
                        // SIEMPRE USER, nunca negociable desde el request
                        // — NFR-SEC-009).
                        // -----------------------------------------------
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // -----------------------------------------------
                        // FR-SEC-VEN-01/02: ambas rutas de Venue publicas.
                        // -----------------------------------------------
                        .requestMatchers(HttpMethod.GET, "/api/venues/**").permitAll()

                        // -----------------------------------------------
                        // FR-SEC-ART-01..03: las tres rutas de Artist
                        // publicas.
                        // -----------------------------------------------
                        .requestMatchers(HttpMethod.GET, "/api/artists/**").permitAll()

                        // -----------------------------------------------
                        // Events: literales especificos primero.
                        // -----------------------------------------------
                        .requestMatchers(HttpMethod.GET, "/api/events/published").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/by-artist").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid")
                                .hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                        // Debe ir DESPUES de los literales de arriba: "*"
                        // calzaria con "published" y "by-artist" si fuera
                        // declarado antes.
                        .requestMatchers(HttpMethod.GET, "/api/events/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")

                        // -----------------------------------------------
                        // Users: FR-SEC-USR-02/03 — cualquier autenticado
                        // puede LLEGAR al Controller; la comparacion
                        // "propio usuario o ADMIN" la hace
                        // AuthorizationService antes de delegar al
                        // Service (anti-IDOR, seccion 11.1 del PRD).
                        // -----------------------------------------------
                        .requestMatchers(HttpMethod.GET, "/api/users/by-email").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/by-username").authenticated()

                        // -----------------------------------------------
                        // Tickets: literal "by-user" ANTES del comodin
                        // "/*" (mismo numero de segmentos que
                        // /api/tickets/{ticketCode}).
                        // -----------------------------------------------
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use")
                                .hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/by-user").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/*").authenticated()

                        // SEC001: denegar por defecto — cualquier ruta no
                        // listada arriba exige autenticacion como minimo.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
