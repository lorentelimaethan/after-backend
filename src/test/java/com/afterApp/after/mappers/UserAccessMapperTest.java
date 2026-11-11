package com.afterApp.after.mappers;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.entity.UserAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAccessMapperTest {

    @Test
    void shouldMapLoginDtoToUserAccess() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ethan");
        dto.setPassword("secret123");

        UserAccess result = UserAccessMapper.fromDto(dto);

        assertEquals("ethan", result.getUsername());
        assertEquals("secret123", result.getPassword());
    }

    @Test
    void shouldMapRegisterDtoToUserAccess() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ethan");
        dto.setPassword("secret123");

        UserAccess result = UserAccessMapper.fromDto(dto);

        assertEquals("ethan", result.getUsername());
        assertEquals("secret123", result.getPassword());
    }
}
