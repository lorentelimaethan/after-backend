package com.afterApp.after.mappers;

import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.dto.UserResponseDTO;
import com.afterApp.after.entity.Users;

public class UserMapper {
    public static UserResponseDTO toDto(Users u){
        UserResponseDTO dto = new UserResponseDTO();

        dto.setId(u.getId());
        dto.setName(u.getName());
        dto.setLastname(u.getLastname());
        dto.setEmail(u.getEmail());
        dto.setPhoneNumber(u.getPhoneNumber());
        dto.setDisplayName(u.getDisplayName());
        if (u.getUserRole() != null) {
            dto.setRoleName(u.getUserRole().getRoleName());
        }

        return dto;
    }

    public static Users updateUserData(Users u, UpdateUserDTO uDtoDetails){
        if(uDtoDetails.getName() != null) {u.setName(uDtoDetails.getName());}
        if(uDtoDetails.getLastname() != null) {u.setLastname(uDtoDetails.getLastname());}
        if(uDtoDetails.getPhoneNumber() != null) {u.setPhoneNumber(uDtoDetails.getPhoneNumber());}
        if(uDtoDetails.getEmail() != null) {u.setEmail(uDtoDetails.getEmail());}

        return u;
    }
}
