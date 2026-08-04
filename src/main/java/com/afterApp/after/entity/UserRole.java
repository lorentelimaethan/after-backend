package com.afterApp.after.entity;

import com.afterApp.after.enums.Resources;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "userRole")
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Getter @Setter
    private String roleName;

    @ElementCollection
    @CollectionTable(name = "user_role_resources", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "resource")
    @Setter @Getter
    private List<Resources> resources;
}
