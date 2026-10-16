package com.pulsepass.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @WithMockUser (el que trae spring-security-test de fabrica) construye
 * un org.springframework.security.core.userdetails.User generico como
 * principal — no tiene getUserId() ni getEmail(), que es justo lo que
 * AuthorizationService necesita para comparar "¿es el dueño del
 * recurso?". Por eso esta anotacion propia: construye un SecurityPrincipal
 * REAL (el mismo tipo que produce JwtAuthenticationFilter en produccion)
 * y lo coloca en el SecurityContext antes de cada test, sin pasar por
 * JWT, login ni base de datos — exactamente lo que pide la seccion 17.2
 * del PRD ("@WithMockUser... cuando corresponda").
 *
 * Uso:
 * <pre>
 * {@code @WithMockSecurityPrincipal(userId = 1L, username = "andrea",
 *         email = "andrea@email.com", roles = "USER")}
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockSecurityPrincipalFactory.class)
public @interface WithMockSecurityPrincipal {

    long userId() default 1L;

    String username() default "andrea";

    String email() default "andrea@email.com";

    String[] roles() default {"USER"};

    boolean active() default true;
}
