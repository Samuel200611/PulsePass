package com.pulsepass.domain;

/**
 * Seccion 9.1 del PRD de Seguridad. Se persiste con EnumType.STRING
 * (nunca el ordinal) en la tabla user_roles.
 *
 * NFR-SEC-009: STAFF y ADMIN nunca se asignan desde el registro publico
 * (POST /api/users) — solo mediante un proceso administrativo o una
 * semilla local controlada (ver V4__security_credentials_roles.sql).
 */
public enum Role {
    USER,
    STAFF,
    ADMIN
}
