package com.pulsepass.controller;

import tools.jackson.databind.ObjectMapper;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)

class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    private static final LocalDateTime EVENT_DATE = LocalDateTime.now().plusMonths(3);

    private CreateEventRequest validRequest() {
        return new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica del Caribe", EventCategory.MUSIC,
                EVENT_DATE, 18, "VEN-SMR-01");
    }

    private EventResponse response(EventStatus status) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica del Caribe", EventCategory.MUSIC, status,
                EVENT_DATE, 18, "VEN-SMR-01", "Marina Convention Center", List.of());
    }

    private EventSummaryResponse summary() {
        return new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, EVENT_DATE,
                "Marina Convention Center");
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-001 — crear valido -> 201
    // ---------------------------------------------------------

    @Test
    void shouldReturn201WhenEventIsCreated() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(response(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-002 — request invalido -> 400
    // ---------------------------------------------------------

    @Test
    void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {
        CreateEventRequest invalid = new CreateEventRequest("", "", null,
                null, null, -1, "");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").exists())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.category").exists())
                .andExpect(jsonPath("$.details.eventDate").exists())
                .andExpect(jsonPath("$.details.venueCode").exists());

        // QT-CTRL-007: un request invalido nunca llega al Service
        verify(eventService, never()).create(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-003 / 004 — consultar existente/inexistente
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenEventExists() throws Exception {
        when(eventService.findByCode("CMF-2026"))
                .thenReturn(response(EventStatus.PUBLISHED));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {
        when(eventService.findByCode("CMF-999"))
                .thenThrow(new ResourceNotFoundException("Event not found: CMF-999"));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: CMF-999"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-005 — listar publicados -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithPublishedEvents() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-006 / 007 — publicar valido/invalido
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenPublishSucceeds() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(response(EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    void shouldReturn409WhenPublishViolatesBusinessRule() throws Exception {
        when(eventService.publish("CMF-2026")).thenThrow(
                new BusinessRuleException("Only DRAFT events can be published. Current status: CANCELLED"));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-008 — asociar artista -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenArtistIsAssociated() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenReturn(response(EventStatus.PUBLISHED));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}",
                        "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    void shouldReturn409WhenArtistAlreadyAssociated() throws Exception {
        when(eventService.addArtist(eq("CMF-2026"), eq(1L))).thenThrow(
                new BusinessRuleException("Artist Solar Beat is already associated with event CMF-2026."));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}",
                        "CMF-2026", 1L))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-009 — buscar por artista -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenSearchingEventsByArtist() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/by-artist")
                        .param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    // ---------------------------------------------------------
    // Extra — tickets pagados de un evento -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithPaidTicketsOfEvent() throws Exception {
        TicketResponse ticket = new TicketResponse(1L, "TCK-ABCD1234",
                TicketType.GENERAL, new BigDecimal("100000.00"), TicketStatus.PAID,
                LocalDateTime.now(), "andrea@email.com", "CMF-2026",
                "Caribbean Music Fest 2026");

        when(ticketService.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-ABCD1234"))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }
}
