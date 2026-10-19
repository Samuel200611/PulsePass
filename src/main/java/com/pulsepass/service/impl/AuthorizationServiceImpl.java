package com.pulsepass.service.impl;

import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthorizationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final String ACCESS_DENIED_MESSAGE =
            "No tiene permisos para realizar esta operación";

    @Override
    public SecurityPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }

        return principal;
    }

    @Override
    public void assertSelfEmailOrAdmin(String email) {
        SecurityPrincipal principal = currentPrincipal();

        boolean self = principal.getEmail().equalsIgnoreCase(email);
        boolean admin = principal.getRoles().contains(Role.ADMIN);

        if (!self && !admin) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    @Override
    public void assertSelfUsernameOrAdmin(String username) {
        SecurityPrincipal principal = currentPrincipal();

        boolean self = principal.getUsername().equalsIgnoreCase(username);
        boolean admin = principal.getRoles().contains(Role.ADMIN);

        if (!self && !admin) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    @Override
    public void assertSelfEmail(String email) {
        SecurityPrincipal principal = currentPrincipal();

        if (!principal.getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    @Override
    public void assertSelfEmailOrStaffOrAdmin(String email) {
        SecurityPrincipal principal = currentPrincipal();

        boolean self = principal.getEmail().equalsIgnoreCase(email);
        boolean privileged = principal.getRoles().contains(Role.STAFF)
                || principal.getRoles().contains(Role.ADMIN);

        if (!self && !privileged) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    @Override
    public boolean canViewEvent(EventStatus status) {
        if (status == EventStatus.PUBLISHED) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
            return false;
        }

        return principal.getRoles().contains(Role.STAFF)
                || principal.getRoles().contains(Role.ADMIN);
    }
}
