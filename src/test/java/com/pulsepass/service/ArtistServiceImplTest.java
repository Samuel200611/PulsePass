package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.service.impl.ArtistServiceImpl;
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
 * BR-ARTIST-001, BR-ARTIST-002.
 */
@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository repository;

    @Mock
    private ArtistMapper mapper;

    @InjectMocks
    private ArtistServiceImpl service;

    private ArtistResponse response() {
        return new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
    }

    @Test
    void shouldFindArtistById() {
        Artist artist = mock(Artist.class);
        ArtistResponse expected = response();

        when(repository.findById(1L)).thenReturn(Optional.of(artist));
        when(mapper.toResponse(artist)).thenReturn(expected);

        assertThat(service.findById(1L)).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundWhenIdDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindArtistByStageName() {
        Artist artist = mock(Artist.class);
        ArtistResponse expected = response();

        when(repository.findByStageNameIgnoreCase("solar beat"))
                .thenReturn(Optional.of(artist));
        when(mapper.toResponse(artist)).thenReturn(expected);

        assertThat(service.findByStageName("solar beat")).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundWhenStageNameDoesNotExist() {
        when(repository.findByStageNameIgnoreCase("Unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByStageName("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnOnlyActiveArtists() {
        Artist artist = mock(Artist.class);
        ArtistResponse expected = response();

        when(repository.findByActiveTrueOrderByStageNameAsc())
                .thenReturn(List.of(artist));
        when(mapper.toResponse(artist)).thenReturn(expected);

        assertThat(service.findActiveArtists()).containsExactly(expected);
    }
}
