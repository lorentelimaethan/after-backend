package com.afterApp.after.mappers;

import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.dto.UserPrivateResponseDTO;
import com.afterApp.after.dto.UserPublicResponseDTO;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    @Test
    void shouldMapPublicProfileWithIdAndDisplayName() {
        UserPublicResponseDTO result = UserMapper.toPublicDto(fullUser());

        assertEquals(42L, result.getId());
        assertEquals("host", result.getDisplayName());
    }

    @Test
    void shouldMapPrivateProfileWithPersonalData() {
        UserPrivateResponseDTO result = UserMapper.toPrivateDto(fullUser());

        assertEquals("Ethan", result.getName());
        assertEquals("Example", result.getLastname());
        assertEquals("host@example.com", result.getEmail());
        assertEquals("123456789", result.getPhoneNumber());
        assertEquals("FREE", result.getRoleName());
    }

    @Test
    void shouldUseUsernameAsDisplayNameWhenRegistering() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("host");
        dto.setName("Ethan");
        dto.setEmail("host@example.com");

        Users result = UserMapper.fromRegisterDto(dto);

        assertEquals("host", result.getDisplayName());
        assertEquals("Ethan", result.getName());
        assertEquals("host@example.com", result.getEmail());
    }

    @Test
    void shouldUpdateOnlyFieldsSentInUpdateDto() {
        Users user = fullUser();
        UpdateUserDTO update = new UpdateUserDTO();
        update.setName("Changed");

        UserMapper.updateUserData(user, update);

        assertEquals("Changed", user.getName());
        assertEquals("Example", user.getLastname());
        assertEquals("host@example.com", user.getEmail());
    }

    private Users fullUser() {
        UserRole role = new UserRole();
        role.setRoleName("FREE");

        Users user = new Users();
        user.setId(42L);
        user.setDisplayName("host");
        user.setName("Ethan");
        user.setLastname("Example");
        user.setEmail("host@example.com");
        user.setPhoneNumber("123456789");
        user.setUserRole(role);
        return user;
    }
}
