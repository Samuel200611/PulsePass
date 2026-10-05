package com.pulsepass.repository;

import com.pulsepass.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de usuarios (FR-USR-001, FR-USR-002).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Seccion 14 del PRD: buscar usuario por email ignorando mayusculas. */
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    List<User> findByActiveTrueOrderByUsernameAsc();

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);
}
