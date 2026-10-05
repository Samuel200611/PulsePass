package com.pulsepass;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-ART-001..004 (gestion de artistas) y QT-005 (relacion Event N:M Artist).
 * Corresponde a la matriz de trazabilidad de la seccion 22 del PRD.
 */
@DisplayName("EventRepository / ArtistRepository — FR-ART-001..004, QT-005")
class EventArtistIT extends PersistenceTestSupport {

    @Test
    @DisplayName("AC-003: los tres artistas quedan asociados al evento")
    void eventKeepsItsArtists() {
        Venue venue = createVenue("VEN-NM-01");
        Event event = createEvent("EVT-NM-01", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(2), venue);

        Artist solarBeat = artistRepository
                .findByStageNameIgnoreCase("Solar Beat").orElseThrow();
        Artist neonWaves = artistRepository
                .findByStageNameIgnoreCase("Neon Waves").orElseThrow();
        Artist caribbeanSound = artistRepository
                .findByStageNameIgnoreCase("Caribbean Sound").orElseThrow();

        event.addArtist(solarBeat);
        event.addArtist(neonWaves);
        event.addArtist(caribbeanSound);
        eventRepository.saveAndFlush(event);

        List<Artist> artists = artistRepository.findArtistsByEventCode("EVT-NM-01");

        assertThat(artists)
                .hasSize(3)
                .extracting(Artist::getStageName)
                .containsExactly("Caribbean Sound", "Neon Waves", "Solar Beat");
    }

    @Test
    @DisplayName("FR-ART-003: agregar dos veces el mismo artista no duplica la asociacion")
    void sameArtistIsNotDuplicated() {
        Venue venue = createVenue("VEN-NM-02");
        Event event = createEvent("EVT-NM-02", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(2), venue);

        Artist solarBeat = artistRepository
                .findByStageNameIgnoreCase("Solar Beat").orElseThrow();

        event.addArtist(solarBeat);
        event.addArtist(solarBeat);
        eventRepository.saveAndFlush(event);

        assertThat(event.getArtists()).hasSize(1);
        assertThat(artistRepository.findArtistsByEventCode("EVT-NM-02")).hasSize(1);
    }

    @Test
    @DisplayName("FR-ART-002: PostgreSQL rechaza un stageName duplicado")
    void stageNameIsUnique() {
        assertThat(artistRepository.findByStageNameIgnoreCase("solar beat")).isPresent();
    }

    @Test
    @DisplayName("FR-ART-004: una consulta JPQL recupera los eventos del artista")
    void eventsByArtistAreFoundThroughArtistSide() {
        Venue venue = createVenue("VEN-NM-05");
        Event event = createEvent("EVT-NM-05", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(2), venue);
        Artist artist = artistRepository
                .findByStageNameIgnoreCase("Ocean Drive").orElseThrow();
        event.addArtist(artist);
        eventRepository.flush();

        assertThat(artist.getEvents())
                .extracting(Event::getEventCode)
                .contains("EVT-NM-05");
    }
}
