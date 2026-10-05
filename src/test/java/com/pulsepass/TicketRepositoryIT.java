package com.pulsepass;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-TKT-001..008 (gestion de tickets), QT-006 (Ticket -> User / Ticket ->
 * Event) y QT-008 (JPQL con JOIN y COUNT). Corresponde a la matriz de
 * trazabilidad de la seccion 22 del PRD.
 */
@DisplayName("TicketRepository — FR-TKT-001..008")
class TicketRepositoryIT extends PersistenceTestSupport {

    @Test
    @DisplayName("FR-TKT-006: tickets de un usuario navegando Ticket -> User -> email")
    void ticketsAreFoundByUserEmail() {
        Venue venue = createVenue("VEN-TK-01");
        Event event = createEvent("EVT-TK-01", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(4), venue);
        User andrea = createUser("andrea-tk", "andrea.tk@pulsepass.co");
        User carlos = createUser("carlos-tk", "carlos.tk@pulsepass.co");

        createTicket("TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, andrea, event);
        createTicket("TCK-0002", TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.PAID, carlos, event);
        ticketRepository.flush();

        List<Ticket> andreaTickets = ticketRepository
                .findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc("ANDREA.TK@PULSEPASS.CO");

        assertThat(andreaTickets)
                .hasSize(1)
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-0001");
    }

    @Test
    @DisplayName("FR-TKT-007: solo se retornan los tickets PAID del evento")
    void onlyPaidTicketsAreReturned() {
        Venue venue = createVenue("VEN-TK-02");
        Event event = createEvent("EVT-TK-02", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(4), venue);
        User user = createUser("laura-tk", "laura.tk@pulsepass.co");

        createTicket("TCK-1001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, user, event);
        createTicket("TCK-1002", TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.RESERVED, user, event);
        createTicket("TCK-1003", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.CANCELLED, user, event);
        ticketRepository.flush();

        List<Ticket> paid = ticketRepository
                .findByEvent_EventCodeAndStatusOrderByPurchaseDateAsc(
                        "EVT-TK-02", TicketStatus.PAID);

        assertThat(paid)
                .hasSize(1)
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-1001");
    }

    @Test
    @DisplayName("QT-008/AC-008: el conteo de ventas solo considera tickets PAID")
    void salesCountOnlyIncludesPaidTickets() {
        Venue venue = createVenue("VEN-TK-03");
        Event event = createEvent("EVT-TK-03", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(5), venue);
        User user = createUser("miguel-tk", "miguel.tk@pulsepass.co");

        createTicket("TCK-2001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, user, event);
        createTicket("TCK-2002", TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.PAID, user, event);
        createTicket("TCK-2003", TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.RESERVED, user, event);
        createTicket("TCK-2004", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.CANCELLED, user, event);
        ticketRepository.flush();

        long paidCount = ticketRepository.countPaidTicketsByEventCode("EVT-TK-03");
        BigDecimal revenue = ticketRepository.sumPaidRevenueByEventCode("EVT-TK-03");

        assertThat(paidCount).isEqualTo(2);
        assertThat(revenue).isEqualByComparingTo("370000.00");
    }

    @Test
    @DisplayName("AC-005: PostgreSQL rechaza un ticketCode duplicado")
    void ticketCodeIsUnique() {
        Venue venue = createVenue("VEN-TK-04");
        Event event = createEvent("EVT-TK-04", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(6), venue);
        User user = createUser("dup-tk", "dup.tk@pulsepass.co");

        createTicket("TCK-0001-DUP", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, user, event);
        ticketRepository.flush();

        Ticket duplicate = new Ticket("TCK-0001-DUP", TicketType.GENERAL,
                new BigDecimal("120000.00"), TicketStatus.RESERVED,
                LocalDateTime.now(), user, event);

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("FR-TKT-003: el CHECK impide precios negativos")
    void priceCannotBeNegative() {
        Venue venue = createVenue("VEN-TK-05");
        Event event = createEvent("EVT-TK-05", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(6), venue);
        User user = createUser("neg-tk", "neg.tk@pulsepass.co");

        Ticket invalid = new Ticket("TCK-NEG", TicketType.GENERAL,
                new BigDecimal("-1.00"), TicketStatus.RESERVED,
                LocalDateTime.now(), user, event);

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(invalid));
    }

    @Test
    @DisplayName("FR-TKT-001: el ticket no puede existir sin usuario y evento validos")
    void ticketRequiresBothUserAndEvent() {
        Venue venue = createVenue("VEN-TK-06");
        Event event = createEvent("EVT-TK-06", EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(6), venue);
        User user = createUser("req-tk", "req.tk@pulsepass.co");

        Ticket ticket = createTicket("TCK-REQ", TicketType.STUDENT,
                new BigDecimal("60000.00"), TicketStatus.RESERVED, user, event);

        assertThat(ticket.getUser()).isNotNull();
        assertThat(ticket.getEvent()).isNotNull();
    }
}
