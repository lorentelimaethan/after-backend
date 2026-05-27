package com.afterApp.after.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDTO {
    private Long id;

    private String name;

    private String lastname;

    private String phoneNumber;

    private String email;

    private String displayName;
}
