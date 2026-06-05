package com.afterApp.after.services;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
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
    private UserAccessRepository userAccessRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAccessServices userAccessServices;

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("Admin");
        dto.setPassword("1234");

        when(userAccessRepository.existsByUsername("Admin"))
                .thenReturn(false);

        when(userAccessRepository.save(any(UserAccess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccess result = userAccessServices.registerUser(dto);

        assertEquals("Admin", result.getUsername());
        assertNotNull(result.getPassword());

        verify(userAccessRepository).save(any(UserAccess.class));
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("Admin");
        dto.setPassword("1234");

        when(userAccessRepository.existsByUsername("Admin"))
                .thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userAccessServices.registerUser(dto)
        );

        assertEquals("Username already exists", ex.getMessage());

        verify(userAccessRepository, never()).save(any());
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

        when(userAccessRepository.findByUsername("Admin"))
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

        when(userAccessRepository.findByUsername("Admin"))
                .thenReturn(Optional.of(access));

        boolean result = userAccessServices.validateUser(dto);

        assertFalse(result);
    }
}
