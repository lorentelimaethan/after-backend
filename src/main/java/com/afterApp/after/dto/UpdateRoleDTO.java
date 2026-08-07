package com.afterApp.after.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateRoleDTO {
    @NotBlank(message = "Role name required")
    private String roleName;
}
