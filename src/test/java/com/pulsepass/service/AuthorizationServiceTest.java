package com.pulsepass.service;

import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.impl.AuthorizationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Seccion 17.1 del PRD: "dueño vs tercero; STAFF en uso; ADMIN en
 * publicacion; rechazo de correos ajenos."
 *
 * No necesita Mockito: AuthorizationServiceImpl no tiene colaboradores,
 * solo lee SecurityContextHolder (igual que lo hace JwtAuthenticationFilter
 * en produccion), asi que el test simplemente lo puebla a mano antes de
 * cada caso.
 */
class AuthorizationServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationServiceImpl();

    @AfterEach
    void clearSecurityContext() {
        // Evita que la autenticacion de un test "se filtre" al siguiente.
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String username, String email, Role... roles) {
        SecurityPrincipal principal = new SecurityPrincipal(
                userId, username, email, "irrelevant-hash", true, Set.of(roles));

        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ---------------------------------------------------------
    // currentPrincipal
    // ---------------------------------------------------------

    @Test
    void shouldThrowAccessDeniedWhenThereIsNoAuthentication() {
        assertThatThrownBy(authorizationService::currentPrincipal)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldReturnThePrincipalWhenAuthenticated() {
        authenticateAs(1L, "andrea", "andrea@email.com", Role.USER);

        SecurityPrincipal principal = authorizationService.currentPrincipal();

        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getEmail()).isEqualTo("andrea@email.com");
    }

    // ---------------------------------------------------------
    // assertSelfEmailOrAdmin — dueño vs tercero, ADMIN
    // ---------------------------------------------------------

    @Test
    void shouldAllowSelfEmail() {
        authenticateAs(1L, "andrea", "andrea@email.com", Role.USER);

        authorizationService.assertSelfEmailOrAdmin("andrea@email.com");
        // No lanza: la asercion implicita es que no hay excepcion.
    }

    @Test
    void shouldAllowSelfEmailIgnoringCase() {
        authenticateAs(1L, "andrea", "Andrea@Email.com", Role.USER);

        authorizationService.assertSelfEmailOrAdmin("ANDREA@EMAIL.COM");
    }

    @Test
    void shouldRejectAnotherUsersEmail() {
        authenticateAs(1L, "andrea", "andrea@email.com", Role.USER);

        assertThatThrownBy(() ->
                authorizationService.assertSelfEmailOrAdmin("carlos@email.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowAdminToAccessAnotherUsersEmail() {
        authenticateAs(99L, "miguel-admin", "miguel@pulsepass.co", Role.ADMIN);

        authorizationService.assertSelfEmailOrAdmin("carlos@email.com");
    }

    // ---------------------------------------------------------
    // assertSelfEmail — sin excepcion para ADMIN (FR-SEC-TKT-01)
    // ---------------------------------------------------------

    @Test
    void shouldRejectAnotherUsersEmailEvenForAdmin() {
        // La ruta de compra ya esta restringida a ROLE_USER en
        // SecurityConfig, pero assertSelfEmail en si misma NO tiene
        // excepcion para ADMIN — es estricta por diseño.
        authenticateAs(99L, "miguel-admin", "miguel@pulsepass.co", Role.ADMIN);

        assertThatThrownBy(() ->
                authorizationService.assertSelfEmail("carlos@email.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------
    // assertSelfEmailOrStaffOrAdmin — STAFF puede ver tickets ajenos
    // ---------------------------------------------------------

    @Test
    void shouldAllowStaffToAccessAnotherUsersTicket() {
        authenticateAs(50L, "laura", "laura@pulsepass.co", Role.STAFF);

        authorizationService.assertSelfEmailOrStaffOrAdmin("carlos@email.com");
    }

    @Test
    void shouldRejectRegularUserAccessingAnotherUsersTicket() {
        authenticateAs(1L, "andrea", "andrea@email.com", Role.USER);

        assertThatThrownBy(() ->
                authorizationService.assertSelfEmailOrStaffOrAdmin("carlos@email.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------
    // assertSelfUsernameOrAdmin
    // ---------------------------------------------------------

    @Test
    void shouldRejectAnotherUsersUsername() {
        authenticateAs(1L, "andrea", "andrea@email.com", Role.USER);

        assertThatThrownBy(() ->
                authorizationService.assertSelfUsernameOrAdmin("carlos"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------
    // canViewEvent — ADMIN en publicacion / visibilidad de borradores
    // ---------------------------------------------------------

    @Test
    void shouldAllowAnyoneToViewPublishedEventsEvenAnonymously() {
        // Sin autenticar: SecurityContextHolder vacio.
        assertThat(authorizationService.canViewEvent(EventStatus.PUBLISHED)).isTrue();
    }

    @Test
    void shouldHideDraftFromAnonymous() {
        assertThat(authorizationService.canViewEvent(EventStatus.DRAFT)).isFalse();
    }

    @Test
    void shouldHideDraftFromRegularUser() {
        authenticateAs(1L, "andrea", "andrea@email.com", Role.USER);

        assertThat(authorizationService.canViewEvent(EventStatus.DRAFT)).isFalse();
    }

    @Test
    void shouldAllowStaffToViewDraft() {
        authenticateAs(50L, "laura", "laura@pulsepass.co", Role.STAFF);

        assertThat(authorizationService.canViewEvent(EventStatus.DRAFT)).isTrue();
    }

    @Test
    void shouldAllowAdminToViewDraft() {
        authenticateAs(99L, "miguel-admin", "miguel@pulsepass.co", Role.ADMIN);

        assertThat(authorizationService.canViewEvent(EventStatus.DRAFT)).isTrue();
    }
}
