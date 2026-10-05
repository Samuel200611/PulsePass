package com.pulsepass;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-EVT-001..006 (gestion de eventos). Corresponde a la matriz de
 * trazabilidad de la seccion 22 del PRD.
 */
@DisplayName("EventRepository — FR-EVT-001..006")
class EventRepositoryIT extends PersistenceTestSupport {

    @Test
    @DisplayName("AC-002: el evento queda asociado a su venue")
    void eventKeepsItsVenue() {
        Venue venue = createVenue("VEN-SMR-02");
        createEvent("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(3), venue);

        Optional<Event> found = eventRepository.findByEventCode("CMF-2026");

        assertThat(found).isPresent();
        assertThat(found.get().getVenue().getCode()).isEqualTo("VEN-SMR-02");
    }

    @Test
    @DisplayName("AC-006: la cartelera solo muestra eventos PUBLISHED, en orden cronologico")
    void publishedEventsAreOrderedByDate() {
        Venue venue = createVenue("VEN-CARTELERA");
        createEvent("EVT-LATE", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(90), venue);
        createEvent("EVT-SOON", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(10), venue);
        createEvent("EVT-DRAFT", EventStatus.DRAFT,
                LocalDateTime.now().plusDays(20), venue);
        createEvent("EVT-CANCELLED", EventStatus.CANCELLED,
                LocalDateTime.now().plusDays(30), venue);
        eventRepository.flush();

        List<Event> published = eventRepository
                .findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(published)
                .extracting(Event::getEventCode)
                .containsSubsequence("EVT-SOON", "EVT-LATE")
                .doesNotContain("EVT-DRAFT", "EVT-CANCELLED");
    }

    @Test
    @DisplayName("FR-EVT-006: streaming_url (agregada por V3) acepta NULL y valores validos")
    void streamingUrlIsNullableAndEditable() {
        Venue venue = createVenue("VEN-V3-01");
        Event event = createEvent("EVT-V3-01", EventStatus.DRAFT,
                LocalDateTime.now().plusMonths(1), venue);

        assertThat(event.getStreamingUrl()).isNull();

        event.setStreamingUrl("https://stream.pulsepass.co/evt-v3-01");
        Event saved = eventRepository.saveAndFlush(event);

        assertThat(saved.getStreamingUrl()).endsWith("evt-v3-01");
    }

    @Test
    @DisplayName("FR-EVT-002: PostgreSQL rechaza un eventCode duplicado")
    void eventCodeIsUnique() {
        Venue venue = createVenue("VEN-DUP-EVT");
        createEvent("EVT-DUP-01", EventStatus.DRAFT,
                LocalDateTime.now().plusDays(5), venue);
        eventRepository.flush();

        assertThat(eventRepository.existsByEventCode("EVT-DUP-01")).isTrue();
    }
}
