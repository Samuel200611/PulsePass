package com.pulsepass.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Recinto donde se realizan los eventos (FR-VEN-001..004).
 * BR-002: un venue puede albergar cero o muchos eventos.
 */
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    /** FR-VEN-003: la BD refuerza capacity > 0 con un CHECK. */
    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private boolean active;

    /**
     * Lado inverso del 1:N. El propietario de la relacion es Event
     * (tiene la FK venue_id). Esta lista es solo de navegacion.
     */
    @OneToMany(mappedBy = "venue")
    private List<Event> events = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected Venue() {
    }

    public Venue(String code, String name, String city,
                 String address, int capacity, boolean active) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.capacity = capacity;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return active;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
