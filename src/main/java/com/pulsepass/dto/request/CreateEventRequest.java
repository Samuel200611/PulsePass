package com.pulsepass.dto.request;

import com.pulsepass.domain.EventCategory;

import java.time.LocalDateTime;

/**
 * FR-SVC-003. El status NO forma parte del request: BR-EVENT-005 fija
 * siempre DRAFT como estado inicial, sin importar lo que el cliente envie.
 */
public record CreateEventRequest(

        String eventCode,

        String name,

        String description,

        EventCategory category,

        LocalDateTime eventDate,

        Integer minimumAge,

        String venueCode

) {
}
