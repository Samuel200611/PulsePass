package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tabla 7.1 del PRD: "cargar identidad, password hash, roles y estado de
 * usuario"; prohibido validar reglas comerciales (eso sigue en los
 * Services de la Fase 2, sin tocar).
 *
 * Dos puntos de entrada:
 * - loadUserByUsername(identifier): lo invoca
 *   AuthenticationManager/DaoAuthenticationProvider durante el LOGIN
 *   (acepta username o email, ver UserRepository).
 * - loadUserById(userId): lo invoca JwtAuthenticationFilter en CADA
 *   request autenticado, porque el JWT solo guarda el "sub" (userId) —
 *   nunca el username (SEC006, 12.1 del PRD: "preferencia: guardar
 *   unicamente identificador (sub)... consultar roles actuales aqui").
 *
 * Ambos metodos reconstruyen el principal desde la base de datos, nunca
 * desde el propio JWT: asi, si alguien revoca un rol o desactiva una
 * cuenta, el cambio aplica desde el siguiente request sin esperar a que
 * el token expire (SEC011).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private final UserCredentialRepository userCredentialRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository userCredentialRepository) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) {
        User user = userRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found for identifier: " + identifier));

        return buildPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found for id: " + userId));

        return buildPrincipal(user);
    }

    private SecurityPrincipal buildPrincipal(User user) {
        UserCredential credential = userCredentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User " + user.getId() + " has no login credentials."));

        return new SecurityPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                credential.getPasswordHash(),
                user.isActive(),
                user.getRoles());
    }
}
