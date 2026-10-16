package com.pulsepass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.security.WithMockSecurityPrincipal;
import com.pulsepass.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-SEC-005/006 (seccion 17.2 del PRD de Seguridad).
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // ---------------------------------------------------------
    // T-SEC-005 — login correcto -> 200 con Bearer
    // ---------------------------------------------------------

    @Test
    void shouldReturn200WithTokenOnSuccessfulLogin() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("signed.jwt.token", "Bearer", 900L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("andrea", "UnaFraseSeguraLarga2026!"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    // ---------------------------------------------------------
    // T-SEC-006 — password incorrecto -> 401 generico
    // ---------------------------------------------------------

    @Test
    void shouldReturn401OnInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username/email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("andrea", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldReturn400WhenLoginRequestIsMissingFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.identifier").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    // ---------------------------------------------------------
    // GET /api/auth/me — FR-SEC-AUTH-010
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(userId = 1L, username = "andrea",
            email = "andrea@email.com", roles = "USER")
    void shouldReturn200WithMinimalAuthenticatedUserInfo() throws Exception {
        when(authService.getAuthenticatedUser(any()))
                .thenReturn(new AuthenticatedUserResponse(
                        1L, "andrea", "andrea@email.com", List.of("USER")));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void shouldReturn401WhenCallingMeWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
