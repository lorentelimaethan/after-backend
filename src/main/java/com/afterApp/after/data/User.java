package com.afterApp.after.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long Id;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String lastname;

    @Getter @Setter
    private String phoneNumber;

    @Getter @Setter
    private String email;

    @Getter @Setter
    private String displayName; //Se copia directamente del Username de UserAcces? user.setDisplayName(userAccess.getUsername());

    @OneToOne
    @JoinColumn(name = "user_access_id")
    @Getter @Setter
    private UserAccess userAccess;
}
