package com.afterApp.after.service;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.loader.UserAccessLoader;
import com.afterApp.after.loader.UserRoleLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccessServicesTest {

    @Mock
    private UserAccessLoader userAccessLoader;

    @Mock
    private UserRoleLoader userRoleLoader;

    @InjectMocks
    private UserAccessServices userAccessServices;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ---------- registerUser ----------

    @Test
    void shouldRegisterUserWithFreeRole() {
        when(userRoleLoader.findByRoleName("FREE")).thenReturn(role("FREE"));
        // saveUserAccess returns the same object it receives
        when(userAccessLoader.saveUserAccess(any(UserAccess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccess result = userAccessServices.registerUser(registerDto());

        assertEquals("ethan", result.getUsername());
        assertEquals("FREE", result.getUser().getUserRole().getRoleName());
    }

    @Test
    void shouldSaveHashedPasswordWhenRegistering() {
        when(userRoleLoader.findByRoleName("FREE")).thenReturn(role("FREE"));
        when(userAccessLoader.saveUserAccess(any(UserAccess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccess result = userAccessServices.registerUser(registerDto());

        assertNotEquals("secret123", result.getPassword());
        assertTrue(encoder.matches("secret123", result.getPassword()));
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        doThrow(new AlreadyExistsException("Username already exists"))
                .when(userAccessLoader).existByUsername(any(UserAccess.class));

        AlreadyExistsException exception = assertThrows(AlreadyExistsException.class,
                () -> userAccessServices.registerUser(registerDto()));

        assertEquals("Username already exists", exception.getMessage());
        verify(userAccessLoader, never()).saveUserAccess(any());
    }

    @Test
    void shouldThrowAlreadyExistsWhenDatabaseRejectsDuplicatedUsername() {
        // Two registrations with the same username at the same time:
        // the first check passes, but the database unique constraint fails on save.
        when(userRoleLoader.findByRoleName("FREE")).thenReturn(role("FREE"));
        when(userAccessLoader.saveUserAccess(any(UserAccess.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(userAccessLoader.usernameExists("ethan")).thenReturn(true);

        AlreadyExistsException exception = assertThrows(AlreadyExistsException.class,
                () -> userAccessServices.registerUser(registerDto()));

        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void shouldRethrowOtherDatabaseErrors() {
        when(userRoleLoader.findByRoleName("FREE")).thenReturn(role("FREE"));
        when(userAccessLoader.saveUserAccess(any(UserAccess.class)))
                .thenThrow(new DataIntegrityViolationException("other constraint"));
        when(userAccessLoader.usernameExists("ethan")).thenReturn(false);

        assertThrows(DataIntegrityViolationException.class,
                () -> userAccessServices.registerUser(registerDto()));
    }

    // ---------- validateUser ----------

    @Test
    void shouldValidateCorrectPassword() {
        when(userAccessLoader.findByUsername(any(UserAccess.class))).thenReturn(Optional.of(savedAccess("secret123")));

        boolean result = userAccessServices.validateUser(loginDto("secret123"));

        assertTrue(result);
    }

    @Test
    void shouldRejectWrongPassword() {
        when(userAccessLoader.findByUsername(any(UserAccess.class))).thenReturn(Optional.of(savedAccess("secret123")));

        boolean result = userAccessServices.validateUser(loginDto("wrong"));

        assertFalse(result);
    }

    @Test
    void shouldRejectUnknownUsername() {
        when(userAccessLoader.findByUsername(any(UserAccess.class))).thenReturn(Optional.empty());

        boolean result = userAccessServices.validateUser(loginDto("secret123"));

        assertFalse(result);
    }

    // ---------- helpers ----------

    private RegisterDTO registerDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ethan");
        dto.setPassword("secret123");
        dto.setName("Ethan");
        dto.setLastname("Lorente");
        dto.setEmail("ethan@example.com");
        dto.setPhoneNumber("+34612345678");
        return dto;
    }

    private LoginDTO loginDto(String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ethan");
        dto.setPassword(password);
        return dto;
    }

    private UserAccess savedAccess(String rawPassword) {
        UserAccess access = new UserAccess();
        access.setUsername("ethan");
        access.setPassword(encoder.encode(rawPassword));
        return access;
    }

    private UserRole role(String name) {
        UserRole role = new UserRole();
        role.setRoleName(name);
        return role;
    }
}
