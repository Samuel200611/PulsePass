package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.WithMockSecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)

class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthorizationService authorizationService;

    private RegisterUserRequest validRequest() {
        return new RegisterUserRequest("andrea", "andrea@email.com",
                "Andrea", "Gomez", "3001112233", "Santa Marta",
                LocalDate.of(2001, 5, 12));
    }

    private UserResponse response() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true,
                "Andrea", "Gomez", "3001112233", "Santa Marta",
                LocalDate.of(2001, 5, 12));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-USR-001 — registrar valido -> 201
    // ---------------------------------------------------------

    @Test
    void shouldReturn201WhenRegistrationSucceeds() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(authService).register(any(RegisterUserRequest.class));
        verify(userService, never()).register(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-USR-002 — email invalido -> 400
    // ---------------------------------------------------------

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        RegisterUserRequest invalidEmail = new RegisterUserRequest("andrea",
                "not-an-email", "UnaFraseSeguraLarga2026!", "Andrea", "Gomez",
                "3001112233", "Santa Marta", LocalDate.of(2001, 5, 12));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.email").exists());

        verify(authService, never()).register(any());
    }


    @Test
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        RegisterUserRequest shortPassword = new RegisterUserRequest("andrea",
                "andrea@email.com", "corta", "Andrea", "Gomez",
                "3001112233", "Santa Marta", LocalDate.of(2001, 5, 12));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").exists());

        verify(authService, never()).register(any());
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        RegisterUserRequest missingFields = new RegisterUserRequest("", "",
                "", "", null, null, null, null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingFields)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists())
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.lastName").exists())
                .andExpect(jsonPath("$.details.birthDate").exists());

        verify(authService, never()).register(any());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-USR-003 — username duplicado -> 409
    // ---------------------------------------------------------

    @Test
    void shouldReturn409WhenUsernameIsDuplicated() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-USR-004 — consultar por email -> 200
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com")
    void shouldReturn200WhenFindingOwnEmail() throws Exception {
        when(userService.findByEmail("andrea@email.com")).thenReturn(response());

        mockMvc.perform(get("/api/users/by-email")
                        .param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(authorizationService).assertSelfEmailOrAdmin("andrea@email.com");
        verify(userService).findByEmail("andrea@email.com");
    }

    @Test
    void shouldReturn404WhenEmailDoesNotExist() throws Exception {
        when(userService.findByEmail("unknown@email.com"))
                .thenThrow(new ResourceNotFoundException("User not found: unknown@email.com"));

        mockMvc.perform(get("/api/users/by-email")
                        .param("email", "unknown@email.com"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // TEST-CTRL-USR-005 — consultar por username -> 200
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(username = "andrea")
    void shouldReturn200WhenFindingOwnUsername() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(response());

        mockMvc.perform(get("/api/users/by-username")
                        .param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(authorizationService).assertSelfUsernameOrAdmin("andrea");
    }


    @Test
    @WithMockSecurityPrincipal(username = "miguel-admin", roles = "ADMIN")
    void shouldAllowAdminToFindAnotherUsersUsername() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(response());

        mockMvc.perform(get("/api/users/by-username")
                        .param("username", "andrea"))
                .andExpect(status().isOk());

        verify(authorizationService).assertSelfUsernameOrAdmin("andrea");
    }

    // ---------------------------------------------------------
    // FR-SEC-USR-02 (anti-IDOR): email ajeno -> 403
    // ---------------------------------------------------------

    @Test
    @WithMockSecurityPrincipal(email = "andrea@email.com")
    void shouldReturn403WhenFindingAnotherUsersEmail() throws Exception {
        org.mockito.Mockito.doThrow(
                        new org.springframework.security.access.AccessDeniedException(
                                "No tiene permisos para realizar esta operación"))
                .when(authorizationService).assertSelfEmailOrAdmin("carlos@email.com");

        mockMvc.perform(get("/api/users/by-email")
                        .param("email", "carlos@email.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(userService, never()).findByEmail(any());
    }

    // ---------------------------------------------------------
    // Sin autenticacion -> 401
    // ---------------------------------------------------------

    @Test
    void shouldReturn401WhenFindingByEmailWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(userService, never()).findByEmail(any());
    }
}
