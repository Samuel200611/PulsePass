package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BR-USER-001..005. Unit test puro: sin Spring, sin PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserServiceImpl service;

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
    // TEST-USER-001 — registro valido
    // ---------------------------------------------------------

    @Test
    void shouldRegisterUserWithProfileInTheSameOperation() {
        UserResponse expected = response();

        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(User.class))).thenReturn(expected);

        UserResponse result = service.register(validRequest());

        assertThat(result).isEqualTo(expected);

        // BR-USER-004: ambos se guardan en la misma operacion (misma
        // transaccion, gracias a @Transactional en el metodo).
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(userProfileRepository).save(any());

        // BR-USER-003: todo usuario nuevo inicia activo
        assertThat(userCaptor.getValue().isActive()).isTrue();
        // El perfil quedo enlazado al usuario antes de guardarse
        assertThat(userCaptor.getValue().getProfile()).isNotNull();
        assertThat(userCaptor.getValue().getProfile().getFirstName()).isEqualTo("Andrea");
    }

    // ---------------------------------------------------------
    // TEST-USER-002 — username duplicado
    // ---------------------------------------------------------

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea");

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-USER-003 — email duplicado
    // ---------------------------------------------------------

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea@email.com");

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // TEST-USER-004 — birth date futura
    // ---------------------------------------------------------

    @Test
    void shouldRejectFutureBirthDate() {
        RegisterUserRequest futureBirthDate = new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Gomez",
                "3001112233", "Santa Marta", LocalDate.now().plusDays(1));

        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(futureBirthDate))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void shouldRejectMissingBirthDate() {
        RegisterUserRequest withoutBirthDate = new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Gomez",
                "3001112233", "Santa Marta", null);

        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(withoutBirthDate))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // Lecturas
    // ---------------------------------------------------------

    @Test
    void shouldFindUserByEmail() {
        User user = mock(User.class);
        UserResponse expected = response();

        when(userRepository.findByEmailIgnoreCase("ANDREA@EMAIL.COM"))
                .thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(expected);

        assertThat(service.findByEmail("ANDREA@EMAIL.COM")).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundWhenEmailDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("unknown@email.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEmail("unknown@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindUserByUsername() {
        User user = mock(User.class);
        UserResponse expected = response();

        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(expected);

        assertThat(service.findByUsername("andrea")).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByUsername("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
