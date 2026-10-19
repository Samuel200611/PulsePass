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


public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(String email);

    List<Ticket> findByUser_EmailIgnoreCaseAndStatusOrderByPurchaseDateDesc(
            String email, TicketStatus status);

    List<Ticket> findByEvent_EventCodeAndStatusOrderByPurchaseDateAsc(
            String eventCode, TicketStatus status);

    List<Ticket> findByEvent_EventCodeAndType(String eventCode, TicketType type);

    List<Ticket> findByEvent_EventDateAfterOrderByEvent_EventDateAsc(LocalDateTime from);

    boolean existsByTicketCode(String ticketCode);

    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            JOIN t.event e
            WHERE e.eventCode = :eventCode
              AND t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);


    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = :status
            """)
    long countByEventCodeAndStatus(@Param("eventCode") String eventCode,
                                   @Param("status") TicketStatus status);


    @Query("""
            SELECT COALESCE(SUM(t.price), 0)
            FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    BigDecimal sumPaidRevenueByEventCode(@Param("eventCode") String eventCode);


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
