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

    @NotBlank(message = "Event must have a valid street")
    @Getter @Setter
    private String street;

    @NotBlank(message = "Event must have a valid street number")
    @Getter @Setter
    private String streetNum;

    @NotBlank(message = "Event must have a valid postal code")
    @Getter @Setter
    private String postalCode;

    @Getter @Setter
    private String aditionalInfo;

    @NotBlank(message = "Event must have a valid city")
    @Getter @Setter
    private String city;

    @NotBlank(message = "Event must have a valid province")
    @Getter @Setter
    private String province;
}
