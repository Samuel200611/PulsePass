package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de venues (FR-VEN-001..004).
 */
public interface VenueRepository extends JpaRepository<Venue, Long> {

    /** FR-VEN-001 / AC-001: recuperar un venue por su codigo de negocio. */
    Optional<Venue> findByCode(String code);

    /** Venues activos de una ciudad, sin distinguir mayusculas. */
    List<Venue> findByCityIgnoreCaseAndActiveTrueOrderByNameAsc(String city);

    /** BR-VENUE-002: todos los venues activos, sin filtrar por ciudad. */
    List<Venue> findByActiveTrueOrderByNameAsc();

    /** Verificacion rapida de unicidad antes de intentar persistir. */
    boolean existsByCode(String code);
}
