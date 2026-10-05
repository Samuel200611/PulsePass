package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-05T14:38:45-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Oracle Corporation)"
)
@Component
public class ArtistMapperImpl implements ArtistMapper {

    @Override
    public ArtistResponse toResponse(Artist artist) {
        if ( artist == null ) {
            return null;
        }

        Long id = null;
        String stageName = null;
        String country = null;
        String genre = null;
        boolean active = false;

        id = artist.getId();
        stageName = artist.getStageName();
        country = artist.getCountry();
        genre = artist.getGenre();
        active = artist.isActive();

        ArtistResponse artistResponse = new ArtistResponse( id, stageName, country, genre, active );

        return artistResponse;
    }
}
