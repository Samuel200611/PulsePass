package com.pulsepass;

import com.pulsepass.domain.Artist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QT-001 / QT-002 (seccion 17 del PRD) y NFR-002 / NFR-003.
 *
 * Verifica que Flyway construye el esquema desde una base vacia y que
 * Hibernate lo valida sin crearlo ni modificarlo.
 */
@DisplayName("Flyway construye el esquema e Hibernate lo valida")
class FlywayMigrationIT extends PersistenceTestSupport {

    @Test
    @DisplayName("QT-001/QT-002: el contexto arranca con el esquema validado")
    void contextLoadsWithValidatedSchema() {
        // Si V1+V3 no coincidieran con las entidades, ddl-auto=validate
        // habria impedido arrancar el contexto antes de llegar aqui.
        assertThat(venueRepository).isNotNull();
        assertThat(eventRepository).isNotNull();
        assertThat(artistRepository).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(userProfileRepository).isNotNull();
        assertThat(ticketRepository).isNotNull();
    }

    @Test
    @DisplayName("QT-001: V2 cargo el catalogo inicial de artistas")
    void v2SeedDataIsPresent() {
        List<Artist> artists = artistRepository.findByActiveTrueOrderByStageNameAsc();

        assertThat(artists)
                .extracting(Artist::getStageName)
                .contains("Solar Beat", "Neon Waves", "Caribbean Sound",
                        "Ocean Drive", "Digital Pulse");
    }
}
