package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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


@WebMvcTest(ArtistController.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    private ArtistResponse response() {
        return new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
    }

    // ---------------------------------------------------------
    // TEST-CTRL-ART-001 — buscar por ID -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenArtistExistsById() throws Exception {
        when(artistService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.country").value("Colombia"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findById(1L);
    }

    // ---------------------------------------------------------
    // TEST-CTRL-ART-002 — ID inexistente -> 404
    // ---------------------------------------------------------

    @Test
    void shouldReturn404WhenArtistIdDoesNotExist() throws Exception {
        when(artistService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 999"));

        mockMvc.perform(get("/api/artists/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 999"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-ART-003 — buscar por stageName -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WhenArtistExistsByStageName() throws Exception {
        when(artistService.findByStageName("Solar Beat")).thenReturn(response());

        mockMvc.perform(get("/api/artists/by-stage-name")
                        .param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    void shouldReturn404WhenStageNameDoesNotExist() throws Exception {
        when(artistService.findByStageName("Unknown"))
                .thenThrow(new ResourceNotFoundException("Artist not found: Unknown"));

        mockMvc.perform(get("/api/artists/by-stage-name")
                        .param("stageName", "Unknown"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-ART-004 — listar activos -> 200
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithActiveArtists() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.length()").value(1));

        verify(artistService).findActiveArtists();
    }
}
