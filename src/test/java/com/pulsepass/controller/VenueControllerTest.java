package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)

class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    private VenueResponse response() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Av. del Rio 100", 3, true);
    }

    // ---------------------------------------------------------
    // TEST-CTRL-VEN-001 — venue existente -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenVenueExists() throws Exception {
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(response());

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.city").value("Santa Marta"))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.active").value(true));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    // ---------------------------------------------------------
    // TEST-CTRL-VEN-002 — venue inexistente -> 404
    // ---------------------------------------------------------

    @Test
    void shouldReturn404WhenVenueDoesNotExist() throws Exception {
        when(venueService.findByCode("VEN-999"))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-999"));

        mockMvc.perform(get("/api/venues/{code}", "VEN-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-999"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-VEN-003 — venues activos -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithActiveVenues() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.length()").value(1));

        verify(venueService).findActiveVenues();
    }
}
