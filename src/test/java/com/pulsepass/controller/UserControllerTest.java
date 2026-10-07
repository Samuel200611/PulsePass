package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
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

        verify(userService).register(any(RegisterUserRequest.class));
    }

    // ---------------------------------------------------------
    // TEST-CTRL-USR-002 — email invalido -> 400
    // ---------------------------------------------------------

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        RegisterUserRequest invalidEmail = new RegisterUserRequest("andrea",
                "not-an-email", "Andrea", "Gomez", "3001112233",
                "Santa Marta", LocalDate.of(2001, 5, 12));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.email").exists());

        verify(userService, never()).register(any());
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        RegisterUserRequest missingFields = new RegisterUserRequest("", "",
                "", "", null, null, null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingFields)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.lastName").exists())
                .andExpect(jsonPath("$.details.birthDate").exists());

        verify(userService, never()).register(any());
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
    void shouldReturn200WhenFindingByEmail() throws Exception {
        when(userService.findByEmail("andrea@email.com")).thenReturn(response());

        mockMvc.perform(get("/api/users/by-email")
                        .param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

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
    void shouldReturn200WhenFindingByUsername() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(response());

        mockMvc.perform(get("/api/users/by-username")
                        .param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }
}
