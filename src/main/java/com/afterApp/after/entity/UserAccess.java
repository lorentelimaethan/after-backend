package com.afterApp.after.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "userAccess", uniqueConstraints = @UniqueConstraint(name = "uk_user_access_username", columnNames = "username"))
public class UserAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private long id;

    @Column
    @Getter @Setter
    private String username;

    @Getter @Setter
    private String password;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    @Getter @Setter
    private Users user;
}
