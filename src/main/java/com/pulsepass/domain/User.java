package com.pulsepass.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Identidad de dominio del asistente (FR-USR-001, FR-USR-002).
 * R-004: NO representa credenciales de seguridad; el MVP no tiene
 * autenticacion ni autorizacion.
 *
 * La tabla se llama "users" porque USER es palabra reservada en PostgreSQL.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false)
    private boolean active;

    /**
     * BR-004: lado inverso del 1:1. El propietario es UserProfile,
     * que tiene la FK user_id marcada como UNIQUE.
     */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private UserProfile profile;

    /** Lado inverso del 1:N con Ticket. */
    @OneToMany(mappedBy = "user")
    private List<Ticket> tickets = new ArrayList<>();

    protected User() {
    }

    public User(String username, String email, boolean active) {
        this.username = username;
        this.email = email;
        this.active = active;
    }

    /**
     * Asigna el perfil manteniendo sincronizados ambos lados del 1:1.
     * FR-USR-003: un usuario no puede tener mas de un perfil.
     */
    public void assignProfile(UserProfile profile) {
        this.profile = profile;
        profile.setUser(this);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
