package com.pulsepass.dto.response;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * No expone User ni Event directamente (SRV-001): solo userEmail,
 * eventCode y eventName.
 */
public record TicketResponse(

        Long id,

        String ticketCode,

        TicketType type,

        BigDecimal price,

        TicketStatus status,

        LocalDateTime purchaseDate,

        String userEmail,

        String eventCode,

        String eventName

) {
}
