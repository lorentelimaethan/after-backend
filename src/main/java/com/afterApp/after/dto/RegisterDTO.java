package com.afterApp.after.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO {

    @NotBlank(message = "Username required")
    private String username;

    @NotBlank(message = "Password required")
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;

}
