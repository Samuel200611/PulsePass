package com.pulsepass.service;

import com.pulsepass.domain.EventStatus;
import com.pulsepass.security.SecurityPrincipal;


public interface AuthorizationService {

    SecurityPrincipal currentPrincipal();

    void assertSelfEmailOrAdmin(String email);

    void assertSelfUsernameOrAdmin(String username);

    void assertSelfEmail(String email);

    void assertSelfEmailOrStaffOrAdmin(String email);

    boolean canViewEvent(EventStatus status);
}
