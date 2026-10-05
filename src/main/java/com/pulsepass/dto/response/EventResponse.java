package com.pulsepass.dto.response;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * No expone Venue ni Set<Artist> ni List<Ticket> (SRV-001): solo
 * venueCode/venueName y la lista ya transformada de ArtistResponse.
 */
public record EventResponse(

        Long id,

        String eventCode,

        String name,

        String description,

        EventCategory category,

        EventStatus status,

        LocalDateTime eventDate,

        int minimumAge,

        String venueCode,

        String venueName,

        List<ArtistResponse> artists

) {
}
