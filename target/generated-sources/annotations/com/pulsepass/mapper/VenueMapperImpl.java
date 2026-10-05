package com.pulsepass.mapper;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-05T16:33:14-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Oracle Corporation)"
)
@Component
public class VenueMapperImpl implements VenueMapper {

    @Override
    public VenueResponse toResponse(Venue venue) {
        if ( venue == null ) {
            return null;
        }

        Long id = null;
        String code = null;
        String name = null;
        String city = null;
        String address = null;
        int capacity = 0;
        boolean active = false;

        id = venue.getId();
        code = venue.getCode();
        name = venue.getName();
        city = venue.getCity();
        address = venue.getAddress();
        capacity = venue.getCapacity();
        active = venue.isActive();

        VenueResponse venueResponse = new VenueResponse( id, code, name, city, address, capacity, active );

        return venueResponse;
    }
}
