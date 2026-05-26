package com.afterApp.after.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressDTO {

    @NotBlank(message = "Event must have a valid street")
    private String street;

    @NotBlank(message = "Event must have a valid street number")
    private String streetNum;

    @NotBlank(message = "Event must have a valid postal code")
    private String postalCode;


    private String additionalInfo;

    @NotBlank(message = "Event must have a valid city")
    private String city;

    @NotBlank(message = "Event must have a valid province")
    private String province;
}
