package com.pulsepass.dto.request;

import com.pulsepass.domain.TicketType;

/**
 * FR-SVC-013. El precio NUNCA viaja en el request: lo calcula el sistema
 * (seccion 28 del PRD / TicketPricingPolicy).
 */
public record PurchaseTicketRequest(

        String userEmail,

        String eventCode,

        TicketType type

) {
}
