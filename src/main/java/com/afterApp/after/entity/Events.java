package com.afterApp.after.entity;

import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "event")
public class Events {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @NotBlank(message = "Name required")
    @Getter @Setter
    private String name;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Event must have a type")
    @Getter @Setter
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Event must have a music Style")
    @Getter @Setter
    private MusicStyle musicStyle;

    @NotBlank(message = "Event must have a description")
    @Getter @Setter
    private String description;

    @NotNull(message = "Event must have a date and time")
    @Future(message = "Event date and time must be in the future")
    @Getter @Setter
    private LocalDateTime dateTime;

    @Positive(message = "Event capacity must be > 0")
    @Getter @Setter
    private int capacity;

    @ManyToOne
    @JoinColumn(name = "host_id")
    @Getter @Setter
    private Users host;

    @ManyToMany
    @Getter @Setter
    @JoinTable(
            name = "events_users",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<Users> users = new HashSet<>();

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    @Valid
    @NotNull(message = "Event must have and address")
    @Getter @Setter
    private Address address;
}

/**
 * FALTA
 * Vibes
 * UNDERGROUND
 * LUXURY
 * CASUAL
 * RAVE
 *
 * o incluso tags dinámicos:
 *
 * rooftop
 * pool
 * sunset
 * students
 * erasmus
 */