package com.pulsepass.security;

import com.pulsepass.domain.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class WithMockSecurityPrincipalFactory
        implements WithSecurityContextFactory<WithMockSecurityPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithMockSecurityPrincipal annotation) {
        Set<Role> roles = Arrays.stream(annotation.roles())
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        SecurityPrincipal principal = new SecurityPrincipal(
                annotation.userId(),
                annotation.username(),
                annotation.email(),
                "{bcrypt}$2a$10$irrelevant.for.these.tests",
                annotation.active(),
                roles);

        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
