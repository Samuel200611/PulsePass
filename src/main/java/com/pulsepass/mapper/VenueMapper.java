package com.pulsepass.mapper;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import org.mapstruct.Mapper;

/**
 * Todos los campos de VenueResponse coinciden por nombre con los getters
 * de Venue (capacity es int, active usa isActive()): MapStruct los mapea
 * automaticamente, sin necesidad de @Mapping explicito.
 */
@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}
