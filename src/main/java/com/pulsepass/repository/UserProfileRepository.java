package com.pulsepass.repository;

import com.pulsepass.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de perfiles (FR-USR-003, FR-USR-004).
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** Query Method navegando la relacion UserProfile -> User -> email. */
    Optional<UserProfile> findByUser_EmailIgnoreCase(String email);

    Optional<UserProfile> findByUser_Username(String username);

    /** Perfiles registrados en una ciudad. */
    List<UserProfile> findByCityIgnoreCaseOrderByLastNameAsc(String city);
}
