package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
 * BR-EVENT-001..011. Unit test puro: sin Spring, sin PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper mapper;

    @InjectMocks
    private EventServiceImpl service;

    private CreateEventRequest validRequest() {
        return new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica del Caribe", EventCategory.MUSIC,
                LocalDateTime.now().plusMonths(3), 18, "VEN-SMR-01");
    }

    private EventResponse response(EventStatus status) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica del Caribe", EventCategory.MUSIC, status,
                LocalDateTime.now().plusMonths(3), 18, "VEN-SMR-01",
                "Marina Convention Center", List.of());
    }

    private Venue activeVenue() {
        return new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Av. del Rio 100", 3, true);
    }

    // ---------------------------------------------------------
    // TEST-EVENT-001 / TEST-EVENT-002 — findByCode
    // ---------------------------------------------------------

    @Test
    void shouldFindEventByCode() {
        Event event = mock(Event.class);
        EventResponse expected = response(EventStatus.PUBLISHED);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(mapper.toResponse(event)).thenReturn(expected);

        assertThat(service.findByCode("CMF-2026")).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findByEventCode("CMF-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("CMF-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CMF-999");

        verify(mapper, never()).toResponse(any());
    }

    // ---------------------------------------------------------
    // TEST-EVENT-003 — crear evento valido
    // ---------------------------------------------------------

    @Test
    void shouldCreateEventInDraftStatus() {
        Venue venue = activeVenue();
        EventResponse expected = response(EventStatus.DRAFT);

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Event.class))).thenReturn(expected);

        EventResponse result = service.create(validRequest());

        assertThat(result).isEqualTo(expected);

        var captor = org.mockito.ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        // BR-EVENT-005: el estado inicial SIEMPRE es DRAFT, sin importar
        // que el request no lo declare.
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void shouldRejectDuplicateEventCodeAndNeverSave() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CMF-2026");

        verify(eventRepository, never()).save(any());
        verify(venueRepository, never()).findByCode(any());
    }

    // ---------------------------------------------------------
    // TEST-EVENT-004 — venue inexistente
    // ---------------------------------------------------------

    @Test
    void shouldThrowResourceNotFoundWhenVenueDoesNotExistAndNeverSave() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("VEN-SMR-01");

        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-EVENT-005 — venue inactivo
    // ---------------------------------------------------------

    @Test
    void shouldRejectCreationInInactiveVenue() {
        Venue inactiveVenue = new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Av. del Rio 100", 3, false);

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01"))
                .thenReturn(Optional.of(inactiveVenue));

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-EVENT-006 — fecha pasada
    // ---------------------------------------------------------

    @Test
    void shouldRejectPastEventDate() {
        Venue venue = activeVenue();
        CreateEventRequest pastRequest = new CreateEventRequest("CMF-2026",
                "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                LocalDateTime.now().minusDays(1), 18, "VEN-SMR-01");

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> service.create(pastRequest))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeMinimumAge() {
        Venue venue = activeVenue();
        CreateEventRequest invalidRequest = new CreateEventRequest("CMF-2026",
                "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusMonths(1), -1, "VEN-SMR-01");

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> service.create(invalidRequest))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-EVENT-007 — publicar DRAFT valido
    // ---------------------------------------------------------

    @Test
    void shouldPublishDraftEvent() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.now().plusMonths(1), 18, venue);
        EventResponse expected = response(EventStatus.PUBLISHED);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(mapper.toResponse(event)).thenReturn(expected);

        EventResponse result = service.publish("CMF-2026");

        assertThat(result).isEqualTo(expected);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    // ---------------------------------------------------------
    // TEST-EVENT-008 — publicar CANCELLED
    // ---------------------------------------------------------

    @Test
    void shouldRejectPublishingWhenEventIsNotDraft() {
        Venue venue = activeVenue();
        Event cancelledEvent = new Event("CMF-2026", "Caribbean Music Fest 2026",
                "desc", EventCategory.MUSIC, EventStatus.CANCELLED,
                LocalDateTime.now().plusMonths(1), 18, venue);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(cancelledEvent));

        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectPublishingWhenVenueIsNoLongerActive() {
        Venue inactiveVenue = new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Av. del Rio 100", 3, false);
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.now().plusMonths(1), 18, inactiveVenue);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // addArtist — BR-EVENT-010 / BR-EVENT-011 (unit test requerido)
    // ---------------------------------------------------------

    @Test
    void shouldAssociateArtistToEvent() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(1), 18, venue);
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);
        EventResponse expected = response(EventStatus.PUBLISHED);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(event)).thenReturn(event);
        when(mapper.toResponse(event)).thenReturn(expected);

        EventResponse result = service.addArtist("CMF-2026", 1L);

        assertThat(result).isEqualTo(expected);
        assertThat(event.getArtists()).contains(artist);
    }

    @Test
    void shouldRejectDuplicateArtistAssociation() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(1), 18, venue);
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);
        event.addArtist(artist);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectAddingArtistToCancelledEvent() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.CANCELLED,
                LocalDateTime.now().plusMonths(1), 18, venue);
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectAddingArtistToFinishedEvent() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.FINISHED,
                LocalDateTime.now().minusDays(1), 18, venue);
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundWhenArtistDoesNotExist() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(1), 18, venue);

        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(artistRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addArtist("CMF-2026", 999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // Lecturas en lista
    // ---------------------------------------------------------

    @Test
    void shouldReturnPublishedEventsAsSummaries() {
        Event event = mock(Event.class);
        EventSummaryResponse summary = new EventSummaryResponse(1L, "CMF-2026",
                "Caribbean Music Fest 2026", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusMonths(1),
                "Marina Convention Center");

        when(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED))
                .thenReturn(List.of(event));
        when(mapper.toSummary(event)).thenReturn(summary);

        assertThat(service.findPublishedEvents()).containsExactly(summary);
    }

    @Test
    void shouldFindEventsByArtist() {
        Artist artist = mock(Artist.class);
        Event event = mock(Event.class);
        EventSummaryResponse summary = new EventSummaryResponse(1L, "CMF-2026",
                "Caribbean Music Fest 2026", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusMonths(1),
                "Marina Convention Center");

        when(artistRepository.findByStageNameIgnoreCase("Solar Beat"))
                .thenReturn(Optional.of(artist));
        when(eventRepository.findEventsByArtistStageName("Solar Beat"))
                .thenReturn(List.of(event));
        when(mapper.toSummary(event)).thenReturn(summary);

        assertThat(service.findByArtist("Solar Beat")).containsExactly(summary);
    }

    @Test
    void shouldThrowResourceNotFoundWhenSearchingEventsOfUnknownArtist() {
        when(artistRepository.findByStageNameIgnoreCase("Unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByArtist("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).findEventsByArtistStageName(any());
    }
}
