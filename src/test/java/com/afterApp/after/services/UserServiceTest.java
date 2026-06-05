package com.afterApp.after.services;

import com.afterApp.after.dto.UpdateDisplayNameDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.dto.UserResponseDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.service.UserServices;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private UserAccessRepository userAccessRepository;

    @InjectMocks
    private UserServices userServices;

    @Test
    void shouldUpdateOwnProfileSuccessfully(){

        Users user = new Users();
        user.setId(1L);
        user.setName("Admin");

        UserAccess access = new UserAccess();
        access.setUsername("Admin");
        access.setUser(user);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("NewName");
        dto.setLastname("NewLast");

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Admin");

        when(userAccessRepository.findByUsername("Admin"))
                .thenReturn(Optional.of(access));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(Users.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result =
                userServices.updateUser(1L, dto, "fake-token");

        assertEquals("NewName", result.getName());
        assertEquals("NewLast", result.getLastname());

        verify(userRepository).save(any(Users.class));
    }

    @Test
    void shouldThrowWhenUserTriesToUpdateAnotherProfile(){

        Users requester = new Users();
        requester.setId(1L);

        Users target;
        target = new Users();
        target.setId(2L);

        UserAccess access = new UserAccess();
        access.setUsername("User");
        access.setUser(requester);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("Hack");

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("User");

        when(userAccessRepository.findByUsername("User"))
                .thenReturn(Optional.of(access));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userServices.updateUser(2L, dto, "fake-token")
        );

        assertEquals(
                "You can only update your own profile",
                ex.getMessage()
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldChangeDisplayNameSuccessfully(){
        Users user = new Users();
        user.setId(1L);
        user.setDisplayName("User");

        UserAccess access = new UserAccess();
        access.setUsername("User");
        access.setUser(user);

        UpdateDisplayNameDTO dto = new UpdateDisplayNameDTO();
        dto.setDisplayName("NewName");

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("User");

        when(userAccessRepository.findByUsername("User"))
                .thenReturn(Optional.of(access));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByDisplayName("NewName"))
                .thenReturn(false);

        when(userRepository.save(any(Users.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result =
                userServices.updateDisplayName(1L, "fake-token", dto);

        assertEquals("NewName", result.getDisplayName());

        verify(userRepository).save(any(Users.class));
    }

    @Test
    void shouldThrowWhenDisplayNameAlreadyExists() {

        Users user = new Users();
        user.setId(1L);

        UserAccess access = new UserAccess();
        access.setUsername("User");
        access.setUser(user);

        UpdateDisplayNameDTO dto = new UpdateDisplayNameDTO();
        dto.setDisplayName("takenName");

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("User");

        when(userAccessRepository.findByUsername("User"))
                .thenReturn(Optional.of(access));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByDisplayName("takenName"))
                .thenReturn(true);

        AlreadyExistsException exception = assertThrows(
                AlreadyExistsException.class,
                () -> userServices.updateDisplayName(1L, "fake-token", dto)
        );

        assertEquals(
                "Already Existing username",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
    }
}
