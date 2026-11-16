package com.afterApp.after.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String lastname;

    @Getter @Setter
    private String phoneNumber;

    @Getter @Setter
    private String email;

    @Getter @Setter
    private String displayName;

    @ManyToOne
    @JoinColumn(name = "role_id")
    @Getter @Setter
    private UserRole userRole;
}
