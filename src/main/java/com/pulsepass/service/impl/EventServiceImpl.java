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


@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {


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


    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {

        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException(
                    "Event code already exists: " + request.eventCode());
        }

        Venue venue = venueRepository
                .findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venue not found: " + request.venueCode()));

        if (!venue.isActive()) {
            throw new BusinessRuleException(
                    "Cannot create an event in an inactive venue: "
                            + request.venueCode());
        }

        if (request.eventDate() == null
                || !request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Event date must be in the future: " + request.eventDate());
        }

        if (request.minimumAge() == null || request.minimumAge() < 0) {
            throw new BusinessRuleException(
                    "Minimum age must be zero or greater.");
        }

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


    @Override
    @Transactional
    public EventResponse publish(String eventCode) {

        Event event = eventRepository
                .findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));


        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: "
                            + event.getStatus());
        }


        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Cannot publish an event whose date is not in the future.");
        }


        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException(
                    "Cannot publish an event whose venue is no longer active.");
        }

        event.setStatus(EventStatus.PUBLISHED);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }


    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {


        Event event = eventRepository
                .findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));

        Artist artist = artistRepository
                .findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artist not found: " + artistId));

        if (ARTIST_FORBIDDEN_STATUSES.contains(event.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot add artists to an event in status "
                            + event.getStatus() + ".");
        }

        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException(
                    "Artist " + artist.getStageName()
                            + " is already associated with event " + eventCode + ".");
        }

        event.addArtist(artist);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }
}
