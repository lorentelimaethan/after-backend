package com.afterApp.after.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "userAccess")
public class UserAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private long id;

    @Getter @Setter
    @Column(unique = true)
    private String username;

    @Getter @Setter
    private String password;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    @Getter @Setter
    private User user;
}

// meter user acces dentro de user solo dejar user acees control y validar user directamente como user o que user accees
// tenga un one to one que useracees tenga aceso a user pero user a acess no. emepezar segundo join column id.