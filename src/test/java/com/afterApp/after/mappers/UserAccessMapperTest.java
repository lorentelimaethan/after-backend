package com.afterApp.after.mappers;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserAccessMapperTest {

    @Test
    void shouldMapLoginCredentialsToUserAccess() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ethan");
        dto.setPassword("secret123");

        var access = UserAccessMapper.fromDto(dto);

        assertEquals("ethan", access.getUsername());
        assertEquals("secret123", access.getPassword());
    }

    @Test
    void shouldMapRegisterCredentialsToUserAccess() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ethan");
        dto.setPassword("secret123");
        dto.setName("Ethan");

        var access = UserAccessMapper.fromDto(dto);

        assertEquals("ethan", access.getUsername());
        assertEquals("secret123", access.getPassword());
    }
}
