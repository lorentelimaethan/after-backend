package com.afterApp.after.mappers;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.entity.UserAccess;

public class UserAccessMapper {
    public static LoginDTO toDto(UserAccess access){
        LoginDTO dto = new LoginDTO();

        dto.setPassword(access.getPassword());
        dto.setUsername(access.getUsername());

        return dto;
    }

   public static UserAccess fromDto(LoginDTO dto){
        UserAccess access = new UserAccess();

        access.setUsername(dto.getUsername());
        access.setPassword(dto.getPassword());

        return access;
   }
}
