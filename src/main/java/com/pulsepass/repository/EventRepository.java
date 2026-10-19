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


public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    List<Event> findByVenue_CodeOrderByEventDateAsc(String venueCode);

    List<Event> findByVenue_CityIgnoreCaseAndStatusOrderByEventDateAsc(
            String city, EventStatus status);

    List<Event> findByCategoryAndStatusOrderByEventDateAsc(
            EventCategory category, EventStatus status);

    List<Event> findByStatusAndEventDateAfterOrderByEventDateAsc(
            EventStatus status, LocalDateTime from);

    List<Event> findByNameContainingIgnoreCaseOrderByEventDateAsc(String fragment);

    boolean existsByEventCode(String eventCode);

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.artists a
            WHERE LOWER(a.stageName) = LOWER(:stageName)
            ORDER BY e.eventDate ASC
            """)
    List<Event> findEventsByArtistStageName(@Param("stageName") String stageName);

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

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.artists
            WHERE e.venue.code = :venueCode
            """)
    List<Event> findByVenueCodeWithArtists(@Param("venueCode") String venueCode);
}
