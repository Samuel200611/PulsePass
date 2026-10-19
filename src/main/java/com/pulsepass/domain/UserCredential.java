package com.pulsepass.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Credencial de login, separada deliberadamente de {@link User}
 * (seccion 9.1 del PRD de Seguridad):
 *
 * 1. evita modificar el contrato de negocio de User (usado por MapStruct
 *    para construir UserResponse en la Fase 2 — si passwordHash viviera
 *    en User, bastaria con que alguien agregara ese campo a UserResponse
 *    para que MapStruct lo serializara por accidente, ya que mapea por
 *    coincidencia de nombre);
 * 2. modela honestamente la relacion 1:1 con PK compartida
 *    (user_id es a la vez PK y FK, vía @MapsId) — "este usuario tiene a
 *    lo sumo una credencial, y su identidad ES su user_id".
 */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    protected UserCredential() {
    }

    public UserCredential(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }
}
