package com.pulsepass;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-VEN-001..004 (gestion de venues). Corresponde a la matriz de
 * trazabilidad de la seccion 22 del PRD.
 */
@DisplayName("VenueRepository — FR-VEN-001..004")
class VenuePersistenceTest extends PersistenceTestSupport {

    @Test
    @DisplayName("AC-001: el venue se recupera por su codigo de negocio")
    void venueIsRetrievedByCode() {
        createVenue("VEN-SMR-01");

        Optional<Venue> found = venueRepository.findByCode("VEN-SMR-01");

        assertThat(found).isPresent();
        assertThat(found.get().getCity()).isEqualTo("Santa Marta");
        assertThat(found.get().getCapacity()).isPositive();
    }

    @Test
    @DisplayName("FR-VEN-002: PostgreSQL rechaza un codigo de venue duplicado")
    void venueCodeIsUnique() {
        createVenue("VEN-DUP-01");

        Venue duplicate = new Venue("VEN-DUP-01", "Otro recinto",
                "Barranquilla", "Calle 10", 3000, true);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("FR-VEN-003: PostgreSQL rechaza capacidad menor o igual a cero")
    void capacityMustBePositive() {
        Venue invalid = new Venue("VEN-BAD", "Sin capacidad",
                "Santa Marta", "Calle 1", 0, true);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(invalid));
    }

    @Test
    @DisplayName("FR-VEN-004: solo se retornan eventos del venue solicitado")
    void eventsAreFilteredByVenueCode() {
        Venue venueA = createVenue("VEN-A");
        Venue venueB = createVenue("VEN-B");
        createEvent("EVT-A1", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(10), venueA);
        createEvent("EVT-B1", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(20), venueB);

        List<Event> events = eventRepository.findByVenue_CodeOrderByEventDateAsc("VEN-A");

        assertThat(events)
                .hasSize(1)
                .extracting(Event::getEventCode)
                .containsExactly("EVT-A1");
    }
}
