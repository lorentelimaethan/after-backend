package com.afterApp.after.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "userAccess")
public class UserAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private long Id;

    @Getter @Setter
    @Column(unique = true)
    private String username;

    @Getter @Setter
    private String password;

    @OneToOne(mappedBy = "UserAccess")
    @Getter @Setter
    private User users;
}
