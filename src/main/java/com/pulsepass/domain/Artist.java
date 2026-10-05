package com.pulsepass.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Artista que participa en eventos (FR-ART-001..004).
 * BR-003: un artista puede participar en cero o muchos eventos.
 */
@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FR-ART-002: identificador de negocio unico. */
    @Column(name = "stage_name", nullable = false, unique = true, length = 150)
    private String stageName;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String genre;

    @Column(nullable = false)
    private boolean active;

    /**
     * Lado inverso del N:M. El propietario es Event (@JoinTable event_artists).
     */
    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();

    protected Artist() {
    }

    public Artist(String stageName, String country, String genre, boolean active) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public String getCountry() {
        return country;
    }

    public String getGenre() {
        return genre;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * equals/hashCode sobre la clave de negocio (stageName) y no sobre el id:
     * el id es null antes del flush, lo que rompe el comportamiento del Set
     * en la relacion N:M.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Artist other)) {
            return false;
        }
        return stageName != null && stageName.equals(other.stageName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stageName);
    }
}
