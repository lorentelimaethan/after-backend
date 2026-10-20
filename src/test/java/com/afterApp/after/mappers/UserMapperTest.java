package com.afterApp.after.mappers;

import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {
    @Test
    void shouldMapPublicAndPrivateProfilesSeparately() {
        Users user = new Users();
        user.setId(42L);
        user.setDisplayName("host");
        user.setName("Ethan");
        user.setLastname("Example");
        user.setEmail("host@example.com");
        user.setPhoneNumber("123456789");
        UserRole role = new UserRole();
        role.setRoleName("FREE");
        user.setUserRole(role);
        var publicDto = UserMapper.toPublicDto(user);
        var privateDto = UserMapper.toPrivateDto(user);
        assertEquals(42L, publicDto.getId());
        assertEquals("host", publicDto.getDisplayName());
        assertEquals("Ethan", privateDto.getName());
        assertEquals("Example", privateDto.getLastname());
        assertEquals("host@example.com", privateDto.getEmail());
        assertEquals("123456789", privateDto.getPhoneNumber());
        assertEquals("FREE", privateDto.getRoleName());
    }

    @Test
    void shouldMapRegistrationAndPreserveUnchangedProfileFields() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("host");
        dto.setName("Ethan");
        dto.setLastname("Example");
        dto.setEmail("host@example.com");
        dto.setPhoneNumber("123456789");
        Users user = UserMapper.fromRegisterDto(dto);
        assertEquals("host", user.getDisplayName());
        assertEquals("Ethan", user.getName());
        assertEquals("Example", user.getLastname());
        assertEquals("host@example.com", user.getEmail());
        assertEquals("123456789", user.getPhoneNumber());
        UpdateUserDTO update = new UpdateUserDTO();
        update.setName("Changed");
        UserMapper.updateUserData(user, update);
        assertEquals("Changed", user.getName());
        assertEquals("host@example.com", user.getEmail());
        assertEquals("Example", user.getLastname());
    }
}
