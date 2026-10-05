package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.TicketServiceImpl;
import com.pulsepass.service.pricing.TicketPricingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BR-TICKET-001..014. Unit test puro: sin Spring, sin PostgreSQL.
 *
 * Reproduce el escenario de aceptacion de la seccion 47 del PRD
 * (Venue VEN-SMR-01, capacity 3; Event CMF-2026; usuarios Andrea/25,
 * Carlos/21, Laura/17, Miguel inactivo).
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final LocalDateTime EVENT_DATE = LocalDateTime.now().plusMonths(3);

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMapper mapper;

    @InjectMocks
    private TicketServiceImpl service;

    // ---------------------------------------------------------
    // Helpers de datos (escenario de la seccion 47 del PRD)
    // ---------------------------------------------------------

    private Venue venue(int capacity) {
        return new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Av. del Rio 100", capacity, true);
    }

    private Event event(EventStatus status, int capacity, int minimumAge) {
        return new Event("CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica del Caribe", EventCategory.MUSIC, status,
                EVENT_DATE, minimumAge, venue(capacity));
    }

    /** Devuelve una fecha de nacimiento que produce exactamente `age` anos
     *  cumplidos en EVENT_DATE, sin depender de la fecha real del sistema. */
    private LocalDate birthDateForAgeAt(int age) {
        return EVENT_DATE.toLocalDate().minusYears(age).minusDays(1);
    }

    private User userOfAge(String email, boolean active, int age) {
        User user = new User(email.substring(0, email.indexOf('@')), email, active);
        UserProfile profile = new UserProfile("Name", "Lastname", null,
                "Santa Marta", birthDateForAgeAt(age));
        user.assignProfile(profile);
        return user;
    }

    private PurchaseTicketRequest requestFor(String email, TicketType type) {
        return new PurchaseTicketRequest(email, "CMF-2026", type);
    }

    private TicketResponse response(TicketStatus status) {
        return new TicketResponse(1L, "TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, status, LocalDateTime.now(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // ---------------------------------------------------------
    // TEST-TICKET-001 — compra valida
    // ---------------------------------------------------------

    @Test
    void shouldPurchaseTicketAsPaid() {
        User andrea = userOfAge("andrea@email.com", true, 25);
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);
        TicketResponse expected = response(TicketStatus.PAID);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(cmf2026));
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026"))
                .thenReturn(0L);
        when(ticketRepository.existsByTicketCode(anyString())).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Ticket.class))).thenReturn(expected);

        TicketResponse result = service.purchase(requestFor("andrea@email.com", TicketType.GENERAL));

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(captor.getValue().getPrice())
                .isEqualByComparingTo(TicketPricingPolicy.BASE_PRICE);

        // No se agoto la capacidad (1 de 3): el evento NO cambia de estado.
        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-TICKET-002 — usuario inexistente
    // ---------------------------------------------------------

    @Test
    void shouldThrowResourceNotFoundWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("unknown@email.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchase(
                requestFor("unknown@email.com", TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-TICKET-003 — usuario inactivo (Miguel, AC-007)
    // ---------------------------------------------------------

    @Test
    void shouldRejectPurchaseFromInactiveUser() {
        User miguel = userOfAge("miguel@email.com", false, 30);

        when(userRepository.findByEmailIgnoreCase("miguel@email.com"))
                .thenReturn(Optional.of(miguel));

        assertThatThrownBy(() -> service.purchase(
                requestFor("miguel@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-TICKET-004 — evento DRAFT
    // ---------------------------------------------------------

    @Test
    void shouldRejectPurchaseForDraftEvent() {
        User andrea = userOfAge("andrea@email.com", true, 25);
        Event draftEvent = event(EventStatus.DRAFT, 3, 18);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> service.purchase(
                requestFor("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-TICKET-005 — evento CANCELLED
    // ---------------------------------------------------------

    @Test
    void shouldRejectPurchaseForCancelledEvent() {
        User andrea = userOfAge("andrea@email.com", true, 25);
        Event cancelledEvent = event(EventStatus.CANCELLED, 3, 18);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(cancelledEvent));

        assertThatThrownBy(() -> service.purchase(
                requestFor("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectPurchaseForPastEvent() {
        User andrea = userOfAge("andrea@email.com", true, 25);
        Event pastEvent = new Event("CMF-2026", "Caribbean Music Fest 2026",
                "desc", EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.now().minusDays(1), 18, venue(3));

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(pastEvent));

        assertThatThrownBy(() -> service.purchase(
                requestFor("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-TICKET-006 — Laura, 17 anios, minimumAge 18 (AC-006)
    // ---------------------------------------------------------

    @Test
    void shouldRejectPurchaseWhenUserIsUnderMinimumAge() {
        User laura = userOfAge("laura@email.com", true, 17);
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);

        when(userRepository.findByEmailIgnoreCase("laura@email.com"))
                .thenReturn(Optional.of(laura));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(cmf2026));

        assertThatThrownBy(() -> service.purchase(
                requestFor("laura@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldSkipAgeValidationWhenEventHasNoMinimumAge() {
        User user = userOfAge("kid@email.com", true, 10);
        Event openEvent = event(EventStatus.PUBLISHED, 3, 0);
        TicketResponse expected = response(TicketStatus.PAID);

        when(userRepository.findByEmailIgnoreCase("kid@email.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(openEvent));
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026"))
                .thenReturn(0L);
        when(ticketRepository.existsByTicketCode(anyString())).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Ticket.class))).thenReturn(expected);

        assertThat(service.purchase(requestFor("kid@email.com", TicketType.GENERAL)))
                .isEqualTo(expected);
    }

    // ---------------------------------------------------------
    // TEST-TICKET-007 — sin capacidad (AC-009)
    // ---------------------------------------------------------

    @Test
    void shouldRejectPurchaseWhenEventHasNoCapacity() {
        User andrea = userOfAge("andrea@email.com", true, 25);
        Event fullEvent = event(EventStatus.PUBLISHED, 3, 18);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(fullEvent));
        // capacity = 3, ya hay 3 pagados: no queda cupo
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026"))
                .thenReturn(3L);

        assertThatThrownBy(() -> service.purchase(
                requestFor("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-TICKET-008 — ultimo ticket agota capacidad (AC-008)
    // ---------------------------------------------------------

    @Test
    void shouldMarkEventAsSoldOutWhenLastTicketIsPurchased() {
        User carlos = userOfAge("carlos@email.com", true, 21);
        Event almostFullEvent = event(EventStatus.PUBLISHED, 3, 18);
        TicketResponse expected = response(TicketStatus.PAID);

        when(userRepository.findByEmailIgnoreCase("carlos@email.com"))
                .thenReturn(Optional.of(carlos));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(almostFullEvent));
        // capacity = 3, ya hay 2 pagados: esta es la ultima disponible
        when(ticketRepository.countPaidTicketsByEventCode("CMF-2026"))
                .thenReturn(2L);
        when(ticketRepository.existsByTicketCode(anyString())).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(almostFullEvent)).thenReturn(almostFullEvent);
        when(mapper.toResponse(any(Ticket.class))).thenReturn(expected);

        TicketResponse result = service.purchase(
                requestFor("carlos@email.com", TicketType.GENERAL));

        assertThat(result).isEqualTo(expected);
        // BR-TICKET-008: el cambio a SOLD_OUT ocurre en la misma operacion
        assertThat(almostFullEvent.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(almostFullEvent);
        verify(ticketRepository).save(any(Ticket.class));
    }

    // ---------------------------------------------------------
    // Lecturas
    // ---------------------------------------------------------

    @Test
    void shouldFindTicketByCode() {
        Ticket ticket = mock(Ticket.class);
        TicketResponse expected = response(TicketStatus.PAID);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(expected);

        assertThat(service.findByCode("TCK-ABCD1234")).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundWhenTicketCodeDoesNotExist() {
        when(ticketRepository.findByTicketCode("TCK-UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("TCK-UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindTicketsByUserEmail() {
        Ticket ticket = mock(Ticket.class);
        TicketResponse expected = response(TicketStatus.PAID);

        when(ticketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(
                "andrea@email.com")).thenReturn(List.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(expected);

        assertThat(service.findByUserEmail("andrea@email.com"))
                .containsExactly(expected);
    }

    @Test
    void shouldFindPaidTicketsByEvent() {
        Ticket ticket = mock(Ticket.class);
        TicketResponse expected = response(TicketStatus.PAID);

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);
        when(ticketRepository.findByEvent_EventCodeAndStatusOrderByPurchaseDateAsc(
                "CMF-2026", TicketStatus.PAID)).thenReturn(List.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(expected);

        assertThat(service.findPaidTicketsByEvent("CMF-2026"))
                .containsExactly(expected);
    }

    @Test
    void shouldThrowResourceNotFoundWhenListingTicketsOfUnknownEvent() {
        when(eventRepository.existsByEventCode("CMF-999")).thenReturn(false);

        assertThatThrownBy(() -> service.findPaidTicketsByEvent("CMF-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------
    // TEST-TICKET-009 — cancelar PAID
    // ---------------------------------------------------------

    @Test
    void shouldCancelPaidTicket() {
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);
        User andrea = userOfAge("andrea@email.com", true, 25);
        Ticket ticket = new Ticket("TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, TicketStatus.PAID,
                LocalDateTime.now(), andrea, cmf2026);
        TicketResponse expected = response(TicketStatus.CANCELLED);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(mapper.toResponse(ticket)).thenReturn(expected);

        TicketResponse result = service.cancel("TCK-ABCD1234");

        assertThat(result).isEqualTo(expected);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
    }

    // ---------------------------------------------------------
    // TEST-TICKET-010 — cancelar USED (AC-011)
    // ---------------------------------------------------------

    @Test
    void shouldRejectCancellingUsedTicket() {
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);
        User andrea = userOfAge("andrea@email.com", true, 25);
        Ticket usedTicket = new Ticket("TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, TicketStatus.USED,
                LocalDateTime.now(), andrea, cmf2026);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(usedTicket));

        assertThatThrownBy(() -> service.cancel("TCK-ABCD1234"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledTicket() {
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);
        User andrea = userOfAge("andrea@email.com", true, 25);
        Ticket cancelledTicket = new Ticket("TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, TicketStatus.CANCELLED,
                LocalDateTime.now(), andrea, cmf2026);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(cancelledTicket));

        assertThatThrownBy(() -> service.cancel("TCK-ABCD1234"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectCancellingAfterEventHasTakenPlace() {
        Event pastEvent = new Event("CMF-2025", "Past Fest", "desc",
                EventCategory.MUSIC, EventStatus.FINISHED,
                LocalDateTime.now().minusDays(1), 18, venue(3));
        User andrea = userOfAge("andrea@email.com", true, 25);
        Ticket ticket = new Ticket("TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, TicketStatus.PAID,
                LocalDateTime.now().minusDays(10), andrea, pastEvent);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel("TCK-ABCD1234"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-TICKET-011 — marcar PAID como usado (AC-010)
    // ---------------------------------------------------------

    @Test
    void shouldMarkPaidTicketAsUsed() {
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);
        User andrea = userOfAge("andrea@email.com", true, 25);
        Ticket ticket = new Ticket("TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, TicketStatus.PAID,
                LocalDateTime.now(), andrea, cmf2026);
        TicketResponse expected = response(TicketStatus.USED);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(mapper.toResponse(ticket)).thenReturn(expected);

        TicketResponse result = service.markAsUsed("TCK-ABCD1234");

        assertThat(result).isEqualTo(expected);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(ticket);
    }

    // ---------------------------------------------------------
    // TEST-TICKET-012 — usar ticket CANCELLED
    // ---------------------------------------------------------

    @Test
    void shouldRejectUsingCancelledTicket() {
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);
        User andrea = userOfAge("andrea@email.com", true, 25);
        Ticket cancelledTicket = new Ticket("TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, TicketStatus.CANCELLED,
                LocalDateTime.now(), andrea, cmf2026);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(cancelledTicket));

        assertThatThrownBy(() -> service.markAsUsed("TCK-ABCD1234"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectUsingAlreadyUsedTicket() {
        Event cmf2026 = event(EventStatus.PUBLISHED, 3, 18);
        User andrea = userOfAge("andrea@email.com", true, 25);
        Ticket usedTicket = new Ticket("TCK-ABCD1234", TicketType.GENERAL,
                TicketPricingPolicy.BASE_PRICE, TicketStatus.USED,
                LocalDateTime.now(), andrea, cmf2026);

        when(ticketRepository.findByTicketCode("TCK-ABCD1234"))
                .thenReturn(Optional.of(usedTicket));

        assertThatThrownBy(() -> service.markAsUsed("TCK-ABCD1234"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundWhenMarkingUnknownTicketAsUsed() {
        when(ticketRepository.findByTicketCode("TCK-UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsUsed("TCK-UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
