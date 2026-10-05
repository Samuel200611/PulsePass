package com.pulsepass.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Perfil individual del usuario (FR-USR-003, FR-USR-004, BR-004).
 * Propietario del 1:1: la FK user_id es UNIQUE, de modo que PostgreSQL
 * impide asociar dos perfiles al mismo usuario (AC-004).
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String city;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    protected UserProfile() {
    }

    public UserProfile(String firstName, String lastName,
                       String phone, String city, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.city = city;
        this.birthDate = birthDate;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    /**
     * Visibilidad de paquete: la sincronizacion del 1:1 debe hacerse
     * siempre a traves de User#assignProfile.
     */
    void setUser(User user) {
        this.user = user;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
