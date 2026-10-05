package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
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
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * FR-SVC-003..008. Por defecto toda la clase es de solo lectura; las
 * operaciones que escriben (create, publish, addArtist) sobrescriben con
 * @Transactional (SRV-004).
 */
@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    /** BR-EVENT-011: no se pueden agregar artistas en estos estados. */
    private static final Set<EventStatus> ARTIST_FORBIDDEN_STATUSES =
            Set.of(EventStatus.CANCELLED, EventStatus.FINISHED);

    private final EventRepository eventRepository;

    private final VenueRepository venueRepository;

    private final ArtistRepository artistRepository;

    private final EventMapper mapper;

    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper mapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
    }

    // =====================================================
    // create — BR-EVENT-001..006
    // =====================================================

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {

        // BR-EVENT-001: codigo unico
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException(
                    "Event code already exists: " + request.eventCode());
        }

        // BR-EVENT-002: el venue debe existir
        Venue venue = venueRepository
                .findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venue not found: " + request.venueCode()));

        // BR-EVENT-003: el venue debe estar activo
        if (!venue.isActive()) {
            throw new BusinessRuleException(
                    "Cannot create an event in an inactive venue: "
                            + request.venueCode());
        }

        // BR-EVENT-004: la fecha debe ser futura
        if (request.eventDate() == null
                || !request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Event date must be in the future: " + request.eventDate());
        }

        // BR-EVENT-006: minimumAge >= 0 (0 = sin restriccion)
        if (request.minimumAge() == null || request.minimumAge() < 0) {
            throw new BusinessRuleException(
                    "Minimum age must be zero or greater.");
        }

        // BR-EVENT-005: el estado inicial SIEMPRE es DRAFT; el request
        // no tiene ni puede tener influencia sobre este valor.
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.category(),
                EventStatus.DRAFT,
                request.eventDate(),
                request.minimumAge(),
                venue);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    // =====================================================
    // Lecturas
    // =====================================================

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventRepository
                .findByEventCode(eventCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository
                .findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        // Validamos que el artista exista para dar un error claro
        // (NFR-005) en vez de devolver silenciosamente una lista vacia.
        artistRepository
                .findByStageNameIgnoreCase(stageName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artist not found: " + stageName));

        return eventRepository
                .findEventsByArtistStageName(stageName)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    // =====================================================
    // publish — BR-EVENT-007..009
    // =====================================================

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {

        Event event = eventRepository
                .findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));

        // BR-EVENT-007: solo se puede publicar desde DRAFT
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: "
                            + event.getStatus());
        }

        // BR-EVENT-008: la fecha debe seguir siendo futura
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Cannot publish an event whose date is not in the future.");
        }

        // BR-EVENT-009: el venue debe seguir activo
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException(
                    "Cannot publish an event whose venue is no longer active.");
        }

        event.setStatus(EventStatus.PUBLISHED);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    // =====================================================
    // addArtist — BR-EVENT-010..011
    // =====================================================

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {

        // 1. buscar evento
        Event event = eventRepository
                .findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));

        // 2. buscar artista
        Artist artist = artistRepository
                .findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artist not found: " + artistId));

        // 3. validar que el evento acepte artistas (BR-EVENT-011)
        if (ARTIST_FORBIDDEN_STATUSES.contains(event.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot add artists to an event in status "
                            + event.getStatus() + ".");
        }

        // 4. evitar duplicados (BR-EVENT-010)
        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException(
                    "Artist " + artist.getStageName()
                            + " is already associated with event " + eventCode + ".");
        }

        // 5. asociar ambos lados de la relacion
        event.addArtist(artist);

        // 6. guardar
        Event saved = eventRepository.save(event);

        // 7. retornar
        return mapper.toResponse(saved);
    }
}
