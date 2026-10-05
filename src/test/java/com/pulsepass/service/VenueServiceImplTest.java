package com.pulsepass.service;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BR-VENUE-001, BR-VENUE-002. Unit test puro: sin Spring, sin PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository repository;

    @Mock
    private VenueMapper mapper;

    @InjectMocks
    private VenueServiceImpl service;

    private VenueResponse response() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Av. del Rio 100", 3, true);
    }

    @Test
    void shouldFindVenueByCode() {
        Venue venue = mock(Venue.class);
        VenueResponse expected = response();

        when(repository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(mapper.toResponse(venue)).thenReturn(expected);

        VenueResponse result = service.findByCode("VEN-SMR-01");

        assertThat(result).isEqualTo(expected);
        verify(repository).findByCode("VEN-SMR-01");
    }

    @Test
    void shouldThrowResourceNotFoundWhenVenueDoesNotExist() {
        when(repository.findByCode("VEN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("VEN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("VEN-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldReturnOnlyActiveVenues() {
        Venue venue = mock(Venue.class);
        VenueResponse expected = response();

        when(repository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of(venue));
        when(mapper.toResponse(venue)).thenReturn(expected);

        List<VenueResponse> result = service.findActiveVenues();

        assertThat(result).containsExactly(expected);
        verify(repository).findByActiveTrueOrderByNameAsc();
    }
}
