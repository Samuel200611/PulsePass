package com.pulsepass;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-SRC-001..004 (descubrimiento y consultas). Corresponde a la matriz
 * de trazabilidad de la seccion 22 del PRD.
 */
@DisplayName("EventRepository / TicketRepository — FR-SRC-001..004")
class EventSearchIT extends PersistenceTestSupport {

    @Test
    @DisplayName("AC-007: cada evento del artista aparece una sola vez (DISTINCT)")
    void eventsByArtistHaveNoDuplicates() {
        Venue venue = createVenue("VEN-SRC-01");
        Artist solarBeat = artistRepository
                .findByStageNameIgnoreCase("Solar Beat").orElseThrow();
        Artist neonWaves = artistRepository
                .findByStageNameIgnoreCase("Neon Waves").orElseThrow();

        Event first = createEvent("EVT-SRC-01", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30), venue);
        first.addArtist(solarBeat);
        first.addArtist(neonWaves);

        Event second = createEvent("EVT-SRC-02", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(60), venue);
        second.addArtist(solarBeat);

        eventRepository.flush();

        List<Event> events = eventRepository.findEventsByArtistStageName("solar beat");

        assertThat(events)
                .hasSize(2)
                .extracting(Event::getEventCode)
                .containsExactly("EVT-SRC-01", "EVT-SRC-02");
    }

    @Test
    @DisplayName("FR-SRC-002: filtra por ciudad del venue y artista")
    void eventsAreFilteredByCityAndArtist() {
        Venue venue = createVenue("VEN-SRC-02");
        Event event = createEvent("EVT-SRC-CITY", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(45), venue);
        Artist artist = artistRepository
                .findByStageNameIgnoreCase("Ocean Drive").orElseThrow();
        event.addArtist(artist);
        eventRepository.flush();

        List<Event> matching = eventRepository
                .findEventsByCityAndArtist("santa marta", "ocean drive");
        List<Event> otherCity = eventRepository
                .findEventsByCityAndArtist("Bogota", "Ocean Drive");

        assertThat(matching)
                .extracting(Event::getEventCode)
                .contains("EVT-SRC-CITY");
        assertThat(otherCity).isEmpty();
    }

    @Test
    @DisplayName("FR-SRC-003: los recomendados son case-insensitive, filtrados y sin duplicados")
    void recommendedEventsUseDistinctAndIgnoreCase() {
        Venue venue = createVenue("VEN-SRC-03");
        Event event = createEvent("EVT-SRC-REC", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(50), venue);
        event.addArtist(artistRepository
                .findByStageNameIgnoreCase("Digital Pulse").orElseThrow());
        event.addArtist(artistRepository
                .findByStageNameIgnoreCase("Neon Waves").orElseThrow());
        eventRepository.flush();

        List<Event> recommended = eventRepository.findRecommendedEvents(
                EventStatus.PUBLISHED,
                LocalDateTime.now(),
                "SANTA MARTA",
                "pulse");

        assertThat(recommended)
                .hasSize(1)
                .extracting(Event::getEventCode)
                .containsExactly("EVT-SRC-REC");
    }

    @Test
    @DisplayName("FR-SRC-004: los tickets de eventos futuros quedan ordenados cronologicamente")
    void ticketsForFutureEventsAreOrderedByEventDate() {
        Venue venue = createVenue("VEN-SRC-04");
        Event soon = createEvent("EVT-SRC-SOON", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(5), venue);
        Event later = createEvent("EVT-SRC-LATER", EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(40), venue);
        User user = createUser("future-tk", "future.tk@pulsepass.co");

        createTicket("TCK-FUT-1", TicketType.GENERAL,
                new BigDecimal("100000.00"), TicketStatus.RESERVED, user, later);
        createTicket("TCK-FUT-2", TicketType.GENERAL,
                new BigDecimal("100000.00"), TicketStatus.RESERVED, user, soon);
        ticketRepository.flush();

        List<Ticket> tickets = ticketRepository
                .findByEvent_EventDateAfterOrderByEvent_EventDateAsc(LocalDateTime.now());

        assertThat(tickets)
                .extracting(t -> t.getEvent().getEventCode())
                .containsExactly("EVT-SRC-SOON", "EVT-SRC-LATER");
    }
}
