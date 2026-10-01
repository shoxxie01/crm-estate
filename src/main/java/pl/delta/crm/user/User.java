package pl.delta.crm.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pl.delta.crm.agency.Agency;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    /** Login użytkownika. Trzymany małymi literami. Porównanie jest wtedy proste i deterministyczne. */
    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    /**
     * Nazwa biura jako tekst. Zostawiona wyłącznie dla zgodności wstecz.
     * Źródłem prawdy jest {@link #agency}; kolumna zniknie osobną migracją,
     * kiedy nic już jej nie czyta.
     */
    @Column(nullable = false, length = 150)
    private String agencyName;

    /**
     * Biuro, do którego należy użytkownik. Po nim filtrujemy dostęp do danych.
     *
     * <p>Pobierane zachłannie celowo: agencja jest potrzebna przy każdym żądaniu
     * do zawężenia zapytań, a {@code open-in-view: false} sprawia, że leniwy
     * proxy i tak wybuchłby poza transakcją filtra uwierzytelniającego.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {
        // wymagane przez JPA
    }

    public User(String email, String passwordHash, String firstName, String lastName,
                Agency agency, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.agency = agency;
        this.agencyName = agency.getName();
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public Agency getAgency() {
        return agency;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
