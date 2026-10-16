package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Seccion 17.1 del PRD: "usuario existente, inexistente, roles
 * actualizados, estado inactivo."
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User userWith(Long id, String username, String email,
                          boolean active, Role... roles) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        when(user.getEmail()).thenReturn(email);
        when(user.isActive()).thenReturn(active);
        when(user.getRoles()).thenReturn(Set.of(roles));
        return user;
    }

    // ---------------------------------------------------------
    // loadUserByUsername — identifier = username o email
    // ---------------------------------------------------------

    @Test
    void shouldLoadExistingUserByIdentifier() {
        User user = userWith(1L, "andrea", "andrea@email.com", true, Role.USER);
        UserCredential credential = new UserCredential(user, "$2a$10$hashed");

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("andrea", "andrea"))
                .thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(1L)).thenReturn(Optional.of(credential));

        UserDetails details = service.loadUserByUsername("andrea");

        assertThat(details).isInstanceOf(SecurityPrincipal.class);
        SecurityPrincipal principal = (SecurityPrincipal) details;
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void shouldThrowWhenIdentifierDoesNotMatchAnyUser() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("unknown", "unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUserHasNoCredentials() {
        User user = userWith(1L, "andrea", "andrea@email.com", true, Role.USER);

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("andrea", "andrea"))
                .thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("andrea"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ---------------------------------------------------------
    // loadUserById — usado por JwtAuthenticationFilter (SEC011)
    // ---------------------------------------------------------

    @Test
    void shouldLoadUserByIdWithFreshRolesAndStatus() {
        User user = userWith(1L, "andrea", "andrea@email.com", true,
                Role.USER, Role.STAFF);
        UserCredential credential = new UserCredential(user, "$2a$10$hashed");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(1L)).thenReturn(Optional.of(credential));

        UserDetails details = service.loadUserById(1L);

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
    }

    @Test
    void shouldReflectDeactivatedUserAsDisabled() {
        // SEC011: un usuario desactivado despues de emitido su token
        // debe reflejarse como no habilitado en el SIGUIENTE request.
        User user = userWith(1L, "andrea", "andrea@email.com", false, Role.USER);
        UserCredential credential = new UserCredential(user, "$2a$10$hashed");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(1L)).thenReturn(Optional.of(credential));

        UserDetails details = service.loadUserById(1L);

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void shouldThrowWhenUserIdNoLongerExists() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById(999L))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
