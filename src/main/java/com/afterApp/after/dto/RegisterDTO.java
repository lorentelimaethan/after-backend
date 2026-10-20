package com.afterApp.after.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO extends LoginDTO {

    @NotBlank(message = "LastName required")
    private String lastname;

    @NotBlank(message = "Name required")
    private String name;

    @NotBlank(message = "Phone number required")
    private String phoneNumber;

    @NotBlank(message = "Email required")
    private String email;

}
