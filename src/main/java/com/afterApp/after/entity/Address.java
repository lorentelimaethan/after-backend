package com.afterApp.after.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "address")
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter @Setter
    private String street;

    @Getter @Setter
    private String streetNum;

    @Getter @Setter
    private String postalCode;

    @Getter @Setter
    private String aditionalInfo;

    @Getter @Setter
    private String city;

    @Getter @Setter
    private String province;
}
