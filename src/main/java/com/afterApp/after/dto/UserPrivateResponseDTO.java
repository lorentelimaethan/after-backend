package com.afterApp.after.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPrivateResponseDTO extends UserPublicResponseDTO {

    private String name;

    private String lastname;

    private String phoneNumber;

    private String email;

    private String roleName;
}
