package com.afterApp.after.entity;

import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private EventType eventType;

    @Getter @Setter
    private MusicStyle musicStyle;

    @Getter @Setter
    private String description;


    @Getter @Setter
    private LocalDateTime dateTime;

    @Getter @Setter
    private int capacity;

    @ManyToOne
    @JoinColumn(name = "host_id")
    @Getter @Setter
    private User host;

    @ManyToMany
    @Getter @Setter
    @JoinTable(
            name = "events_users",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
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