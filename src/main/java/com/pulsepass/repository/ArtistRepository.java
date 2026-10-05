package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de artistas (FR-ART-001..004).
 */
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /** FR-ART-002: busqueda por clave de negocio, case-insensitive. */
    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    /** Artistas activos ordenados alfabeticamente. */
    List<Artist> findByActiveTrueOrderByStageNameAsc();

    /** Busqueda parcial por nombre artistico. */
    List<Artist> findByStageNameContainingIgnoreCase(String fragment);

    /**
     * FR-ART-003: artistas que participan en un evento dado.
     * JPQL con JOIN sobre la relacion N:M, navegando desde Artist.
     */
    @Query("""
            SELECT a
            FROM Artist a
            JOIN a.events e
            WHERE e.eventCode = :eventCode
            ORDER BY a.stageName ASC
            """)
    List<Artist> findArtistsByEventCode(@Param("eventCode") String eventCode);
}
