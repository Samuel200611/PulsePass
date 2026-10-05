package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.User;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.pricing.TicketPricingPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/**
 * FR-SVC-013..018. El flujo de compra completo (seccion 24/54 del PRD)
 * corre dentro de una unica @Transactional: si cualquier paso falla, la
 * transaccion entera hace rollback (seccion 39 del PRD).
 */
@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final UserRepository userRepository;

    private final EventRepository eventRepository;

    private final TicketRepository ticketRepository;

    private final TicketMapper mapper;

    public TicketServiceImpl(UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketRepository ticketRepository,
                             TicketMapper mapper) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
    }

    // =====================================================
    // purchase — BR-TICKET-001..009
    // =====================================================

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {

        // 1-2. BR-TICKET-001: el usuario debe existir
        User user = userRepository
                .findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + request.userEmail()));

        // BR-TICKET-002: el usuario debe estar activo
        if (!user.isActive()) {
            throw new BusinessRuleException(
                    "Inactive user cannot purchase tickets: " + request.userEmail());
        }

        // 3. BR-TICKET-003: el evento debe existir
        Event event = eventRepository
                .findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + request.eventCode()));

        // BR-TICKET-004: el evento debe estar PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Cannot purchase tickets for an event in status "
                            + event.getStatus() + ".");
        }

        // BR-TICKET-005: el evento no puede haber ocurrido ya
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Cannot purchase tickets for a past event.");
        }

        // BR-TICKET-006: edad minima, evaluada en la fecha del evento
        validateMinimumAge(user, event);

        // BR-TICKET-007: capacidad disponible
        long paidTickets = ticketRepository
                .countPaidTicketsByEventCode(event.getEventCode());
        int capacity = event.getVenue().getCapacity();

        if (paidTickets >= capacity) {
            throw new BusinessRuleException(
                    "Event " + event.getEventCode() + " has no available capacity.");
        }

        // Calcular precio (BR-TICKET-009: nunca negativo, logica encapsulada
        // en TicketPricingPolicy)
        BigDecimal price = TicketPricingPolicy.calculatePrice(request.type());

        // Crear y guardar el ticket. Para esta version simplificada toda
        // compra valida genera PAID directamente (seccion 27 del PRD).
        Ticket ticket = new Ticket(
                generateTicketCode(),
                request.type(),
                price,
                TicketStatus.PAID,
                LocalDateTime.now(),
                user,
                event);

        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra agota la capacidad, el evento pasa
        // a SOLD_OUT en la MISMA transaccion.
        if (paidTickets + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return mapper.toResponse(saved);
    }

    /**
     * BR-TICKET-006: solo se valida cuando el evento exige una edad
     * minima (minimumAge > 0; 0 significa sin restriccion, BR-EVENT-006).
     * La edad se calcula con la fecha de nacimiento del perfil, evaluada
     * en la fecha del evento (no en "hoy").
     */
    private void validateMinimumAge(User user, Event event) {
        if (event.getMinimumAge() <= 0) {
            return;
        }

        int age = Period.between(
                user.getProfile().getBirthDate(),
                event.getEventDate().toLocalDate()
        ).getYears();

        if (age < event.getMinimumAge()) {
            throw new BusinessRuleException(
                    "User does not meet minimum age for event "
                            + event.getEventCode() + ": requires "
                            + event.getMinimumAge() + ", has " + age + ".");
        }
    }

    /**
     * Genera un codigo de ticket legible y (con altisima probabilidad)
     * unico; se reintenta en el caso extremadamente improbable de colision.
     */
    private String generateTicketCode() {
        String code;
        do {
            code = "TCK-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();
        } while (ticketRepository.existsByTicketCode(code));
        return code;
    }

    // =====================================================
    // Lecturas
    // =====================================================

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository
                .findByTicketCode(ticketCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository
                .findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        if (!eventRepository.existsByEventCode(eventCode)) {
            throw new ResourceNotFoundException("Event not found: " + eventCode);
        }

        return ticketRepository
                .findByEvent_EventCodeAndStatusOrderByPurchaseDateAsc(
                        eventCode, TicketStatus.PAID)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // =====================================================
    // cancel — BR-TICKET-010..012
    // =====================================================

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {

        Ticket ticket = ticketRepository
                .findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + ticketCode));

        // BR-TICKET-010 / BR-TICKET-011: solo un ticket PAID puede
        // cancelarse (esto ya excluye USED y CANCELLED).
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: "
                            + ticket.getStatus());
        }

        // BR-TICKET-012: no se puede cancelar despues de la fecha del evento
        if (!ticket.getEvent().getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Cannot cancel a ticket after the event has taken place.");
        }

        ticket.changeStatus(TicketStatus.CANCELLED);

        Ticket saved = ticketRepository.save(ticket);

        return mapper.toResponse(saved);
    }

    // =====================================================
    // markAsUsed — BR-TICKET-013..014
    // =====================================================

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {

        Ticket ticket = ticketRepository
                .findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + ticketCode));

        // BR-TICKET-013 / BR-TICKET-014: solo PAID puede marcarse como
        // usado (esto ya excluye CANCELLED, que nunca puede usarse).
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used. Current status: "
                            + ticket.getStatus());
        }

        ticket.changeStatus(TicketStatus.USED);

        Ticket saved = ticketRepository.save(ticket);

        return mapper.toResponse(saved);
    }
}
