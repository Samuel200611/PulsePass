package com.pulsepass.dto.response;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;

import java.time.LocalDateTime;

/**
 * Version ligera de EventResponse para listados (cartelera, busqueda por
 * artista): sin description ni artists, para no traer datos de mas.
 */
public record EventSummaryResponse(

        Long id,

        String eventCode,

        String name,

        EventCategory category,

        EventStatus status,

        LocalDateTime eventDate,

        String venueName

) {
}
