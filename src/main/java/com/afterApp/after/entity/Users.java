package com.afterApp.after.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String lastname;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone")
    @Getter @Setter
    private String phoneNumber;

    @Email(message = "Invalid email")
    @Getter @Setter
    private String email;

    @Getter @Setter
    private String displayName;
}
