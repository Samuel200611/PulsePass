package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    private PurchaseTicketRequest validRequest() {
        return new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);
    }

    private TicketResponse response(TicketStatus status) {
        return new TicketResponse(1L, "TCK-ABCD1234", TicketType.GENERAL,
                new BigDecimal("100000.00"), status, LocalDateTime.now(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-001 — compra valida -> 201
    // ---------------------------------------------------------

    @Test
    void shouldReturn201WhenPurchaseSucceeds() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(response(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABCD1234"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value("andrea@email.com"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-002 — request invalido -> 400
    // ---------------------------------------------------------

    @Test
    void shouldReturn400WhenPurchaseRequestIsInvalid() throws Exception {
        PurchaseTicketRequest invalid = new PurchaseTicketRequest("not-an-email", "", null);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.userEmail").exists())
                .andExpect(jsonPath("$.details.eventCode").exists())
                .andExpect(jsonPath("$.details.type").exists());

        verify(ticketService, never()).purchase(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-003 — usuario inexistente -> 404
    // ---------------------------------------------------------

    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@email.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: andrea@email.com"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-004 — regla de negocio -> 409
    // ---------------------------------------------------------

    @Test
    void shouldReturn409WhenBusinessRuleIsViolated() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "User does not meet minimum age for event CMF-2026: requires 18, has 17."));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-005 — consultar ticket -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenTicketExists() throws Exception {
        when(ticketService.findByCode("TCK-ABCD1234")).thenReturn(response(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABCD1234"));

        verify(ticketService).findByCode("TCK-ABCD1234");
    }

    @Test
    void shouldReturn404WhenTicketCodeDoesNotExist() throws Exception {
        when(ticketService.findByCode("TCK-UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: TCK-UNKNOWN"));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-006 — tickets por usuario -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithTicketsOfUser() throws Exception {
        when(ticketService.findByUserEmail("andrea@email.com"))
                .thenReturn(List.of(response(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user")
                        .param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("andrea@email.com"));

        verify(ticketService).findByUserEmail("andrea@email.com");
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-008 / 009 — cancelar valido/invalido
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenCancelSucceeds() throws Exception {
        when(ticketService.cancel("TCK-ABCD1234")).thenReturn(response(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-ABCD1234");
    }

    @Test
    void shouldReturn409WhenCancellingNonPaidTicket() throws Exception {
        when(ticketService.cancel("TCK-ABCD1234")).thenThrow(
                new BusinessRuleException("Only PAID tickets can be cancelled. Current status: USED"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-ABCD1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-TKT-010 / 011 — usar valido/invalido
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenMarkAsUsedSucceeds() throws Exception {
        when(ticketService.markAsUsed("TCK-ABCD1234")).thenReturn(response(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-ABCD1234");
    }

    @Test
    void shouldReturn409WhenMarkingNonPaidTicketAsUsed() throws Exception {
        when(ticketService.markAsUsed("TCK-ABCD1234")).thenThrow(
                new BusinessRuleException("Only PAID tickets can be marked as used. Current status: CANCELLED"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-ABCD1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
