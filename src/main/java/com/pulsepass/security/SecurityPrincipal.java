package com.pulsepass.security;

import com.pulsepass.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

/**
 * Implementacion propia de UserDetails (seccion 14.5 del PRD de
 * Seguridad). El UserDetails estandar de Spring Security solo expone
 * getUsername(); aqui necesitamos ademas:
 *
 * - userId: es lo unico que viaja en el claim "sub" del JWT (SEC006: no
 *   confiar en userEmail/userId que venga del cliente, solo en lo que
 *   JwtAuthenticationFilter reconstruyo a partir de un JWT ya validado);
 * - email: AuthorizationService lo usa constantemente para comparar
 *   "¿el recurso solicitado pertenece a este principal?" (FR-SEC-TKT-01..05).
 *
 * isEnabled() delega en el estado "active" del User real: si
 * CustomUserDetailsService lo reconstruye en cada request (como hace
 * JwtAuthenticationFilter), un usuario desactivado despues de emitido su
 * token deja de poder autenticarse en el siguiente request (SEC011).
 */
public class SecurityPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final Set<Role> roles;

    public SecurityPrincipal(Long userId, String username, String email,
                             String passwordHash, boolean active, Set<Role> roles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // hasRole("ADMIN") en SecurityConfig compara contra "ROLE_ADMIN":
        // Spring Security antepone el prefijo "ROLE_" automaticamente al
        // evaluar hasRole(...), por eso la authority SI debe llevarlo.
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
