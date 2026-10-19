package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final TicketService ticketService;
    private final AuthorizationService authorizationService;

    public EventController(EventService eventService,
                           TicketService ticketService,
                           AuthorizationService authorizationService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
        this.authorizationService = authorizationService;
    }


    // POST /api/events -> 201 Created.

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequest request) {

        EventResponse response = eventService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/events/{eventCode} -> 200 OK, o 404.
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        EventResponse response = eventService.findByCode(eventCode);

        if (!authorizationService.canViewEvent(response.status())) {
            throw new ResourceNotFoundException("Event not found: " + eventCode);
        }

        return ResponseEntity.ok(response);
    }

    // GET /api/events/published -> 200 OK con solo los eventos PUBLISHED.
    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    // PATCH /api/events/{eventCode}/publish -> 200 OK, o 409

    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    // POST /api/events/{eventCode}/artists/{artistId} -> 200 OK, o 409
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(
            @PathVariable String eventCode,
            @PathVariable Long artistId) {

        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }

    // GET /api/events/by-artist?stageName=... -> 200 OK.
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(
            @RequestParam String stageName) {

        List<EventSummaryResponse> events = eventService.findByArtist(stageName);

        List<EventSummaryResponse> visible = events.stream()
                .filter(event -> authorizationService.canViewEvent(event.status()))
                .toList();

        return ResponseEntity.ok(visible);
    }

    // GET /api/events/{eventCode}/tickets/paid -> 200 OK.
    @GetMapping("/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTickets(
            @PathVariable String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }
}
