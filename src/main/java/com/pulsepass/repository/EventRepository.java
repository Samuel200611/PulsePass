package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de eventos (FR-EVT-*, FR-VEN-004, FR-SRC-*).
 *
 * Criterio aplicado (NFR-007): las consultas de uno o dos filtros directos
 * se resuelven con Query Methods; las que cruzan varias asociaciones o
 * requieren DISTINCT usan @Query con JPQL.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    // =====================================================
    // Query Methods
    // =====================================================

    /** FR-EVT-002 / AC-002: buscar evento por su codigo de negocio. */
    Optional<Event> findByEventCode(String eventCode);

    /** FR-EVT-005 / AC-006: cartelera de eventos publicados. */
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    /** FR-VEN-004: eventos de un venue, navegando Event -> Venue -> code. */
    List<Event> findByVenue_CodeOrderByEventDateAsc(String venueCode);

    /** Eventos de una ciudad, navegando Event -> Venue -> city. */
    List<Event> findByVenue_CityIgnoreCaseAndStatusOrderByEventDateAsc(
            String city, EventStatus status);

    /** Cartelera filtrada por categoria. */
    List<Event> findByCategoryAndStatusOrderByEventDateAsc(
            EventCategory category, EventStatus status);

    /** Eventos publicados posteriores a una fecha. */
    List<Event> findByStatusAndEventDateAfterOrderByEventDateAsc(
            EventStatus status, LocalDateTime from);

    /** Busqueda parcial por nombre del evento. */
    List<Event> findByNameContainingIgnoreCaseOrderByEventDateAsc(String fragment);

    boolean existsByEventCode(String eventCode);

    // =====================================================
    // Consultas JPQL
    // =====================================================

    /**
     * FR-SRC-001 / FR-ART-004 / AC-007: eventos donde participa un artista.
     * JOIN sobre la relacion N:M. DISTINCT evita que un evento aparezca
     * repetido cuando el join multiplica filas.
     */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.artists a
            WHERE LOWER(a.stageName) = LOWER(:stageName)
            ORDER BY e.eventDate ASC
            """)
    List<Event> findEventsByArtistStageName(@Param("stageName") String stageName);

    /**
     * FR-SRC-002: eventos de una ciudad en los que participa un artista.
     * Recorre dos asociaciones distintas: Event -> Venue y Event -> Artist.
     */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE LOWER(v.city) = LOWER(:city)
              AND LOWER(a.stageName) = LOWER(:stageName)
            ORDER BY e.eventDate ASC
            """)
    List<Event> findEventsByCityAndArtist(@Param("city") String city,
                                          @Param("stageName") String stageName);

    /**
     * FR-SRC-003: eventos recomendados.
     * Publicados, posteriores a una fecha, en una ciudad y cuyo artista
     * contenga un texto (case-insensitive), sin duplicados y en orden
     * cronologico.
     */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = :status
              AND e.eventDate > :from
              AND LOWER(v.city) = LOWER(:city)
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistFragment, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommendedEvents(@Param("status") EventStatus status,
                                      @Param("from") LocalDateTime from,
                                      @Param("city") String city,
                                      @Param("artistFragment") String artistFragment);

    /**
     * Cartelera de un venue con sus artistas cargados en una sola consulta,
     * evitando el problema N+1 al recorrer los resultados.
     */
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.artists
            WHERE e.venue.code = :venueCode
            """)
    List<Event> findByVenueCodeWithArtists(@Param("venueCode") String venueCode);
}
