package com.afterApp.after.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "userAccess")
public class UserAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private long id;

    @Column(unique = true)
    @NotBlank(message = "Username required")
    @Getter @Setter
    private String username;

    @NotBlank(message = "Password required")
    @Size(min = 6, message = "Password must contain at least 6 characters")
    @Getter @Setter
    private String password;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    @Getter @Setter
    private User user;
}

// meter user acces dentro de user solo dejar user acees control y validar user directamente como user o que user accees
// tenga un one to one que useracees tenga aceso a user pero user a acess no. emepezar segundo join column id.