package com.afterApp.after.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

    @NotBlank(message = "Username required")
    private String username;

    @NotBlank(message = "Password required")
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;

    @NotBlank(message = "LastName required")
    private String lastname;

    @NotBlank(message = "Name required")
    private String name;

    @NotBlank(message = "Phone number required")
    private String phoneNumber;

    @NotBlank(message = "Email required")
    private String email;

    private String displayName;
}
