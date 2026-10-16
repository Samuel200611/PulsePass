package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Seccion 17.1 del PRD: "credenciales correctas, password incorrecto,
 * usuario inactivo y emision solo tras autenticacion."
 *
 * Unit test puro: sin Spring, sin PostgreSQL. AuthenticationManager se
 * mockea — este test NO prueba que Spring Security valide BCrypt
 * correctamente (eso es responsabilidad probada de Spring Security
 * mismo), prueba que AuthServiceImpl reacciona bien a lo que
 * AuthenticationManager le devuelve o le lanza.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterUserRequest registerRequest() {
        return new RegisterUserRequest("andrea", "andrea@email.com",
                "UnaFraseSeguraLarga2026!", "Andrea", "Gomez",
                "3001112233", "Santa Marta", LocalDate.of(2001, 5, 12));
    }

    private UserResponse userResponse() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true,
                "Andrea", "Gomez", "3001112233", "Santa Marta",
                LocalDate.of(2001, 5, 12));
    }

    // ---------------------------------------------------------
    // register — orquestacion atomica (seccion 9.3 del PRD)
    // ---------------------------------------------------------

    @Test
    void shouldOrchestrateUserProfileCredentialAndRoleOnRegister() {
        User persistedUser = mock(User.class);

        when(userService.register(any(RegisterUserRequest.class))).thenReturn(userResponse());
        when(userRepository.findById(1L)).thenReturn(Optional.of(persistedUser));
        when(passwordEncoder.encode("UnaFraseSeguraLarga2026!")).thenReturn("$2a$10$hashed");

        UserResponse result = authService.register(registerRequest());

        assertThat(result).isEqualTo(userResponse());

        // 1. UserService.register sigue aplicando BR-USER-001..005 sin
        // que AuthService las duplique.
        verify(userService).register(any(RegisterUserRequest.class));

        // 2. Se crea la credencial con el hash, nunca con la contraseña
        // en texto plano.
        ArgumentCaptor<UserCredential> credentialCaptor =
                ArgumentCaptor.forClass(UserCredential.class);
        verify(userCredentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");

        // 3. NFR-SEC-009: SIEMPRE ROLE_USER, nunca negociable desde el
        // request (que ademas ni siquiera tiene un campo "role").
        verify(persistedUser).addRole(Role.USER);
        verify(userRepository).save(persistedUser);
    }

    @Test
    void shouldNeverExposePasswordHashInTheResponse() {
        User persistedUser = mock(User.class);

        when(userService.register(any(RegisterUserRequest.class))).thenReturn(userResponse());
        when(userRepository.findById(1L)).thenReturn(Optional.of(persistedUser));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");

        UserResponse result = authService.register(registerRequest());

        // UserResponse (record) ni siquiera TIENE un componente para el
        // hash: este test documenta esa garantia estructuralmente, no
        // solo por inspeccion.
        assertThat(result).isInstanceOf(UserResponse.class);
        assertThat(result.toString()).doesNotContain("hashed");
    }

    // ---------------------------------------------------------
    // login — credenciales correctas
    // ---------------------------------------------------------

    @Test
    void shouldReturnTokenWhenCredentialsAreCorrect() {
        SecurityPrincipal principal = new SecurityPrincipal(
                1L, "andrea", "andrea@email.com", "$2a$10$hashed", true, Set.of(Role.USER));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateAccessToken(1L)).thenReturn("signed.jwt.token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(new LoginRequest("andrea", "correct-password"));

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    // ---------------------------------------------------------
    // login — password incorrecto (FR-SEC-AUTH-003)
    // ---------------------------------------------------------

    @Test
    void shouldThrowInvalidCredentialsWhenPasswordIsWrong() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("andrea", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new UsernameNotFoundException("No user found"));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("unknown", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }

    // ---------------------------------------------------------
    // login — usuario inactivo (FR-SEC-AUTH-004)
    // ---------------------------------------------------------

    @Test
    void shouldThrowInvalidCredentialsWhenUserIsInactive() {
        // DaoAuthenticationProvider lanza DisabledException cuando
        // SecurityPrincipal.isEnabled() es false — AuthServiceImpl la
        // atrapa igual que cualquier otra AuthenticationException, con
        // el MISMO mensaje generico (no revela que la cuenta existe pero
        // esta inactiva).
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("andrea", "correct-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageNotContaining("disabled")
                .hasMessageNotContaining("inactive");

        verify(jwtService, never()).generateAccessToken(any());
    }

    // ---------------------------------------------------------
    // emision solo tras autenticacion exitosa
    // ---------------------------------------------------------

    @Test
    void shouldOnlyGenerateTokenAfterSuccessfulAuthentication() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        // authenticate() se invoco, pero JwtService jamas se toco.
        verify(authenticationManager).authenticate(any());
        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).getAccessTokenTtlSeconds();
    }
}
