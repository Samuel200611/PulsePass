package com.pulsepass.controller;

import com.pulsepass.security.WithMockSecurityPrincipal;
import com.pulsepass.service.AuthorizationService;
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

    @MockitoBean
    private AuthorizationService authorizationService;

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
    // TEST-CTRL-EVT-001 — crear valido -> 201 (ADMIN, FR-SEC-EVT-01)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(roles = "ADMIN")
    void shouldReturn201WhenAdminCreatesEvent() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(response(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    void shouldReturn401WhenAnonymousTriesToCreateEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(eventService, never()).create(any());
    }

    @Test
    @WithMockSecurityPrincipal(roles = "USER")
    void shouldReturn403WhenUserTriesToCreateEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // El rechazo ocurre en la cadena de Spring Security, ANTES del
        // Controller: el Service jamas se invoca.
        verify(eventService, never()).create(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-002 — request invalido -> 400 (con ADMIN, para
    // aislar que el 400 es por Bean Validation y no por el rol)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(roles = "ADMIN")
    void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {
        CreateEventRequest invalid = new CreateEventRequest("", "", null,
                null, null, -1, "");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").exists());

        verify(eventService, never()).create(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-003/004 — consultar evento publicado (anonimo) y
    // evento inexistente
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenAnonymousViewsPublishedEvent() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(response(EventStatus.PUBLISHED));
        when(authorizationService.canViewEvent(EventStatus.PUBLISHED)).thenReturn(true);

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));
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
    // FR-SEC-EVT-02: un DRAFT no es visible para un anonimo (se oculta
    // con 404, no con 403 — no revela su existencia)
    // ---------------------------------------------------------

    @Test
    void shouldHideDraftEventFromAnonymousAs404() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(response(EventStatus.DRAFT));
        when(authorizationService.canViewEvent(EventStatus.DRAFT)).thenReturn(false);

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @WithMockSecurityPrincipal(roles = "STAFF")
    void shouldAllowStaffToViewDraftEvent() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(response(EventStatus.DRAFT));
        when(authorizationService.canViewEvent(EventStatus.DRAFT)).thenReturn(true);

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-005 — listar publicados -> 200 (publico)
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithPublishedEvents() throws Exception {
        when(eventService.findPublishedEvents())
                .thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-006/007 — publicar valido (ADMIN) / invalido (409)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(roles = "ADMIN")
    void shouldReturn200WhenAdminPublishesEvent() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(response(EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    @WithMockSecurityPrincipal(roles = "ADMIN")
    void shouldReturn409WhenPublishViolatesBusinessRule() throws Exception {
        when(eventService.publish("CMF-2026")).thenThrow(
                new BusinessRuleException("Only DRAFT events can be published. Current status: CANCELLED"));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @WithMockSecurityPrincipal(roles = "USER")
    void shouldReturn403WhenUserTriesToPublishEvent() throws Exception {
        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isForbidden());

        verify(eventService, never()).publish(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-008 — asociar artista -> 200 (ADMIN)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(roles = "ADMIN")
    void shouldReturn200WhenAdminAssociatesArtist() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenReturn(response(EventStatus.PUBLISHED));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}",
                        "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    @WithMockSecurityPrincipal(roles = "ADMIN")
    void shouldReturn409WhenArtistAlreadyAssociated() throws Exception {
        when(eventService.addArtist(eq("CMF-2026"), eq(1L))).thenThrow(
                new BusinessRuleException("Artist Solar Beat is already associated with event CMF-2026."));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}",
                        "CMF-2026", 1L))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-EVT-009 — buscar por artista -> 200 (publico, filtrado)
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithOnlyPublishedEventsForAnonymous() throws Exception {
        EventSummaryResponse published = summary();
        EventSummaryResponse draft = new EventSummaryResponse(2L, "EVT-DRAFT",
                "Evento sin publicar", EventCategory.MUSIC, EventStatus.DRAFT,
                EVENT_DATE, "Marina Convention Center");

        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(published, draft));
        when(authorizationService.canViewEvent(EventStatus.PUBLISHED)).thenReturn(true);
        when(authorizationService.canViewEvent(EventStatus.DRAFT)).thenReturn(false);

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));
    }

    // ---------------------------------------------------------
    // Extra — tickets pagados de un evento -> 200 (STAFF/ADMIN)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(roles = "STAFF")
    void shouldReturn200WithPaidTicketsOfEventForStaff() throws Exception {
        TicketResponse ticket = new TicketResponse(1L, "TCK-ABCD1234",
                TicketType.GENERAL, new BigDecimal("100000.00"), TicketStatus.PAID,
                LocalDateTime.now(), "andrea@email.com", "CMF-2026",
                "Caribbean Music Fest 2026");

        when(ticketService.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-ABCD1234"));
    }

    @Test
    @WithMockSecurityPrincipal(roles = "USER")
    void shouldReturn403WhenUserRequestsPaidTicketsOfEvent() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).findPaidTicketsByEvent(any());
    }
}
