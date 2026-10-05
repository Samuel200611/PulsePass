package com.pulsepass.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada emitida a un usuario para un evento (FR-TKT-001..008).
 *
 * BR-006: Ticket es una ENTIDAD y no un @ManyToMany entre User y Event
 * porque la relacion tiene datos propios (ticketCode, type, price, status,
 * purchaseDate). Una tabla de union simple no podria almacenarlos.
 *
 * BR-005: pertenece a exactamente un User y exactamente un Event.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FR-TKT-002: identificador de negocio unico. */
    @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketType type;

    /**
     * BR-007 / NFR-008: precio con BigDecimal + NUMERIC(12,2).
     * Nunca float ni double.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    protected Ticket() {
    }

    public Ticket(String ticketCode, TicketType type, BigDecimal price,
                  TicketStatus status, LocalDateTime purchaseDate,
                  User user, Event event) {
        this.ticketCode = ticketCode;
        this.type = type;
        this.price = price;
        this.status = status;
        this.purchaseDate = purchaseDate;
        this.user = user;
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketType getType() {
        return type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public User getUser() {
        return user;
    }

    public Event getEvent() {
        return event;
    }

    /** Transicion de estado del ticket (seccion 9.2 del PRD). */
    public void changeStatus(TicketStatus status) {
        this.status = status;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
