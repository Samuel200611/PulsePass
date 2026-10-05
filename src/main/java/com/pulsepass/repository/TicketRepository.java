package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de entradas (FR-TKT-*, FR-SRC-004).
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // =====================================================
    // Query Methods
    // =====================================================

    /** FR-TKT-002 / AC-005: recuperar ticket por su codigo unico. */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /** FR-TKT-006: tickets de un usuario, navegando Ticket -> User -> email. */
    List<Ticket> findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(String email);

    /** FR-TKT-006: variante filtrando ademas por estado. */
    List<Ticket> findByUser_EmailIgnoreCaseAndStatusOrderByPurchaseDateDesc(
            String email, TicketStatus status);

    /**
     * FR-TKT-007: tickets pagados de un evento.
     * Se resuelve como Query Method porque son solo dos filtros directos
     * (uno navegando una asociacion): el nombre sigue siendo legible y
     * no necesita JPQL (NFR-007).
     */
    List<Ticket> findByEvent_EventCodeAndStatusOrderByPurchaseDateAsc(
            String eventCode, TicketStatus status);

    /** Tickets de un evento por tipo (GENERAL, VIP, ...). */
    List<Ticket> findByEvent_EventCodeAndType(String eventCode, TicketType type);

    /** FR-SRC-004: tickets cuyo evento es posterior a una fecha. */
    List<Ticket> findByEvent_EventDateAfterOrderByEvent_EventDateAsc(LocalDateTime from);

    boolean existsByTicketCode(String ticketCode);

    // =====================================================
    // Consultas JPQL
    // =====================================================

    /**
     * FR-TKT-008 / AC-008: conteo de tickets PAID de un evento.
     * JPQL con COUNT: solo se necesita el numero, no las entidades, asi
     * que traer la lista completa para hacer size() seria un desperdicio.
     */
    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            JOIN t.event e
            WHERE e.eventCode = :eventCode
              AND t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    /**
     * Conteo parametrizado por estado, util para comparar PAID vs RESERVED.
     */
    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = :status
            """)
    long countByEventCodeAndStatus(@Param("eventCode") String eventCode,
                                   @Param("status") TicketStatus status);

    /**
     * Ingreso confirmado de un evento. COALESCE evita devolver null
     * cuando el evento todavia no tiene tickets pagados.
     */
    @Query("""
            SELECT COALESCE(SUM(t.price), 0)
            FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    BigDecimal sumPaidRevenueByEventCode(@Param("eventCode") String eventCode);

    /**
     * Tickets de un usuario con evento y venue cargados en la misma
     * consulta, para listar la cartelera personal sin caer en N+1.
     */
    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.event e
            JOIN FETCH e.venue
            WHERE LOWER(t.user.email) = LOWER(:email)
            ORDER BY e.eventDate ASC
            """)
    List<Ticket> findUserTicketsWithEventDetails(@Param("email") String email);
}
