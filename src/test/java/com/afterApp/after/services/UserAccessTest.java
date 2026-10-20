package com.afterApp.after.services;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.loader.UserAccessLoader;
import com.afterApp.after.loader.UserRoleLoader;
import com.afterApp.after.service.UserAccessServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccessServicesTest {

    @Mock
    private UserAccessLoader userAccessLoader;

    @Mock
    private UserRoleLoader userRoleLoader;

    @InjectMocks
    private UserAccessServices userAccessServices;

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterDTO dto = registerDto();

        UserRole freeRole = new UserRole();
        freeRole.setRoleName("FREE");

        when(userRoleLoader.findByRoleName("FREE"))
                .thenReturn(freeRole);

        when(userAccessLoader.saveUserAccess(any(UserAccess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccess result = userAccessServices.registerUser(dto);

        assertEquals("Admin", result.getUsername());
        assertNotNull(result.getPassword());
        assertTrue(new BCryptPasswordEncoder(16).matches("1234", result.getPassword()));
        assertEquals("FREE", result.getUser().getUserRole().getRoleName());

        verify(userAccessLoader).saveUserAccess(any(UserAccess.class));
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {

        RegisterDTO dto = registerDto();

        doThrow(new BadRequestException("Username already exists"))
                .when(userAccessLoader)
                .existByUsername(any(UserAccess.class));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userAccessServices.registerUser(dto)
        );

        assertEquals("Username already exists", ex.getMessage());

        verify(userAccessLoader, never()).saveUserAccess(any());
    }

    @Test
    void shouldValidateUserSuccessfully() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("Admin");
        dto.setPassword("1234");

        UserAccess access = new UserAccess();
        access.setUsername("Admin");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);
        access.setPassword(encoder.encode("1234"));

        when(userAccessLoader.findByUsername(any(UserAccess.class)))
                .thenReturn(Optional.of(access));

        boolean result = userAccessServices.validateUser(dto);

        assertTrue(result);
    }

    @Test
    void shouldFailWhenPasswordIsWrong() {

        LoginDTO dto = new LoginDTO();
        dto.setUsername("Admin");
        dto.setPassword("wrong");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);

        UserAccess access = new UserAccess();
        access.setUsername("Admin");
        access.setPassword(encoder.encode("1234"));

        when(userAccessLoader.findByUsername(any(UserAccess.class)))
                .thenReturn(Optional.of(access));

        boolean result = userAccessServices.validateUser(dto);

        assertFalse(result);
    }

    private RegisterDTO registerDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("Admin");
        dto.setPassword("1234");
        dto.setName("Admin");
        dto.setLastname("User");
        dto.setEmail("admin@example.com");
        dto.setPhoneNumber("+34612345678");
        return dto;
    }
}
