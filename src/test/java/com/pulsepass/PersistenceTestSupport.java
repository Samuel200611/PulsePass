package com.pulsepass;

import com.pulsepass.domain.*;
import com.pulsepass.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Punto de partida comun para todas las clases de prueba de la capa de
 * persistencia. No contiene ningun @Test: solo expone los repositories y
 * los helpers de datos (escenario de la seccion 16 del PRD), para que cada
 * clase concreta pueda enfocarse en sus propios casos sin repetir setup.
 *
 * @Transactional aqui se hereda por todas las subclases: cada prueba hace
 * rollback al terminar y no contamina a las demas.
 */
@Transactional
public abstract class PersistenceTestSupport extends AbstractIntegrationTest {

    @Autowired
    protected VenueRepository venueRepository;
    @Autowired
    protected EventRepository eventRepository;
    @Autowired
    protected ArtistRepository artistRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected UserProfileRepository userProfileRepository;
    @Autowired
    protected TicketRepository ticketRepository;

    protected Venue createVenue(String code) {
        return venueRepository.save(new Venue(
                code, "Marina Convention Center", "Santa Marta",
                "Av. del Rio 100", 5000, true));
    }

    protected Event createEvent(String eventCode, EventStatus status,
                                LocalDateTime date, Venue venue) {
        return eventRepository.save(new Event(
                eventCode, "Caribbean Music Fest 2026",
                "Festival de musica del Caribe",
                EventCategory.MUSIC, status, date, 18, venue));
    }

    protected User createUser(String username, String email) {
        return userRepository.save(new User(username, email, true));
    }

    protected Ticket createTicket(String code, TicketType type, BigDecimal price,
                                  TicketStatus status, User user, Event event) {
        return ticketRepository.save(new Ticket(
                code, type, price, status, LocalDateTime.now(), user, event));
    }
}
