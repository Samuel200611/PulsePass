package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.WithMockSecurityPrincipal;
import com.pulsepass.service.AuthorizationService;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)

class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private AuthorizationService authorizationService;

    private PurchaseTicketRequest validRequest() {
        return new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);
    }

    private TicketResponse response(TicketStatus status, String userEmail) {
        return new TicketResponse(1L, "TCK-ABCD1234", TicketType.GENERAL,
                new BigDecimal("100000.00"), status, LocalDateTime.now(),
                userEmail, "CMF-2026", "Caribbean Music Fest 2026");
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-001 — compra valida -> 201 (ROLE_USER, propia)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn201WhenPurchaseSucceeds() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(response(TicketStatus.PAID, "andrea@email.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABCD1234"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(authorizationService).assertSelfEmail("andrea@email.com");
        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void shouldReturn401WhenAnonymousTriesToPurchase() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @WithMockSecurityPrincipal(roles = "STAFF")
    void shouldReturn403WhenStaffTriesToPurchase() throws Exception {
        // FR-SEC-TKT-01 restringe la compra a ROLE_USER; STAFF no puede
        // comprar aunque este autenticado.
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).purchase(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-002 — request invalido -> 400
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(roles = "USER")
    void shouldReturn400WhenPurchaseRequestIsInvalid() throws Exception {
        PurchaseTicketRequest invalid = new PurchaseTicketRequest("not-an-email", "", null);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.userEmail").exists());

        verify(ticketService, never()).purchase(any());
    }

    // ---------------------------------------------------------
    // FR-SEC-TKT-01 (anti-IDOR) — UC-SEC-05: comprar con email ajeno -> 403
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn403WhenPurchasingWithSomeoneElsesEmail() throws Exception {
        PurchaseTicketRequest forCarlos =
                new PurchaseTicketRequest("carlos@email.com", "CMF-2026", TicketType.GENERAL);

        doThrow(new AccessDeniedException("No tiene permisos para realizar esta operación"))
                .when(authorizationService).assertSelfEmail("carlos@email.com");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forCarlos)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // 0 tickets creados: el Service nunca se invoca.
        verify(ticketService, never()).purchase(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-003/004 — usuario inexistente / regla de negocio
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@email.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn409WhenBusinessRuleIsViolated() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "User does not meet minimum age for event CMF-2026: requires 18, has 17."));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-005 — consultar ticket propio -> 200
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn200WhenOwnerViewsOwnTicket() throws Exception {
        when(ticketService.findByCode("TCK-ABCD1234"))
                .thenReturn(response(TicketStatus.PAID, "andrea@email.com"));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABCD1234"));

        verify(authorizationService).assertSelfEmailOrStaffOrAdmin("andrea@email.com");
    }

    @Test
    @WithMockSecurityPrincipal(roles = "USER")
    void shouldReturn404WhenTicketCodeDoesNotExist() throws Exception {
        when(ticketService.findByCode("TCK-UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: TCK-UNKNOWN"));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // FR-SEC-TKT-02 (anti-IDOR) — UC-SEC-06: ticket ajeno -> 403
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn403WhenViewingAnotherUsersTicket() throws Exception {
        when(ticketService.findByCode("TCK-CARLOS"))
                .thenReturn(response(TicketStatus.PAID, "carlos@email.com"));
        doThrow(new AccessDeniedException("No tiene permisos para realizar esta operación"))
                .when(authorizationService).assertSelfEmailOrStaffOrAdmin("carlos@email.com");

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-CARLOS"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockSecurityPrincipal(email = "laura@email.com", roles = "STAFF")
    void shouldAllowStaffToViewAnyTicket() throws Exception {
        when(ticketService.findByCode("TCK-CARLOS"))
                .thenReturn(response(TicketStatus.PAID, "carlos@email.com"));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-CARLOS"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-006 — tickets por usuario -> 200 (propio)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn200WithOwnTickets() throws Exception {
        when(ticketService.findByUserEmail("andrea@email.com"))
                .thenReturn(List.of(response(TicketStatus.PAID, "andrea@email.com")));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("andrea@email.com"));

        verify(authorizationService).assertSelfEmailOrAdmin("andrea@email.com");
    }

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn403WhenRequestingAnotherUsersTickets() throws Exception {
        doThrow(new AccessDeniedException("No tiene permisos para realizar esta operación"))
                .when(authorizationService).assertSelfEmailOrAdmin("carlos@email.com");

        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@email.com"))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).findByUserEmail(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-008/009 — cancelar valido (propio) / invalido (409)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn200WhenOwnerCancelsOwnTicket() throws Exception {
        when(ticketService.findByCode("TCK-ABCD1234"))
                .thenReturn(response(TicketStatus.PAID, "andrea@email.com"));
        when(ticketService.cancel("TCK-ABCD1234"))
                .thenReturn(response(TicketStatus.CANCELLED, "andrea@email.com"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Autorizacion ANTES de mutar (FR-SEC-TKT-05): se consulta primero.
        verify(ticketService).findByCode("TCK-ABCD1234");
        verify(authorizationService).assertSelfEmailOrAdmin("andrea@email.com");
        verify(ticketService).cancel("TCK-ABCD1234");
    }

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn409WhenCancellingNonPaidTicket() throws Exception {
        when(ticketService.findByCode("TCK-ABCD1234"))
                .thenReturn(response(TicketStatus.USED, "andrea@email.com"));
        when(ticketService.cancel("TCK-ABCD1234")).thenThrow(
                new BusinessRuleException("Only PAID tickets can be cancelled. Current status: USED"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-ABCD1234"))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------
    // FR-SEC-TKT-05 (anti-IDOR) — cancelar ticket ajeno -> 403, SIN mutar
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn403WhenCancellingAnotherUsersTicketAndNeverMutate() throws Exception {
        when(ticketService.findByCode("TCK-CARLOS"))
                .thenReturn(response(TicketStatus.PAID, "carlos@email.com"));
        doThrow(new AccessDeniedException("No tiene permisos para realizar esta operación"))
                .when(authorizationService).assertSelfEmailOrAdmin("carlos@email.com");

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-CARLOS"))
                .andExpect(status().isForbidden());

        // El ticket NUNCA cambia de estado si la autorizacion falla.
        verify(ticketService, never()).cancel(any());
    }

    @Test
    @WithMockSecurityPrincipal(roles = "ADMIN")
    void shouldAllowAdminToCancelAnyTicket() throws Exception {
        when(ticketService.findByCode("TCK-CARLOS"))
                .thenReturn(response(TicketStatus.PAID, "carlos@email.com"));
        when(ticketService.cancel("TCK-CARLOS"))
                .thenReturn(response(TicketStatus.CANCELLED, "carlos@email.com"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-CARLOS"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-010/011 — usar valido (STAFF) / invalido (409)
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(roles = "STAFF")
    void shouldReturn200WhenStaffMarksTicketAsUsed() throws Exception {
        when(ticketService.markAsUsed("TCK-ABCD1234"))
                .thenReturn(response(TicketStatus.USED, "andrea@email.com"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-ABCD1234");
    }

    @Test
    @WithMockSecurityPrincipal(roles = "STAFF")
    void shouldReturn409WhenMarkingNonPaidTicketAsUsed() throws Exception {
        when(ticketService.markAsUsed("TCK-ABCD1234")).thenThrow(
                new BusinessRuleException("Only PAID tickets can be marked as used. Current status: CANCELLED"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-ABCD1234"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com", roles = "USER")
    void shouldReturn403WhenUserTriesToMarkTicketAsUsed() throws Exception {
        // FR-SEC-TKT-06: solo STAFF/ADMIN, ni siquiera el propio dueño.
        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-ABCD1234"))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).markAsUsed(any());
    }
}
