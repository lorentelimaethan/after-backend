package com.afterApp.after.services;

import com.afterApp.after.dto.UpdateDisplayNameDTO;
import com.afterApp.after.dto.UpdateRoleDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.dto.UserResponseDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.loader.UserLoader;
import com.afterApp.after.loader.UserRoleLoader;
import com.afterApp.after.service.UserServices;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private UserLoader userLoader;

    @Mock
    private UserRoleLoader userRoleLoader;

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

        when(userLoader.findByUsername("Admin"))
                .thenReturn(access);

        when(userLoader.findById(1L))
                .thenReturn(user);

        when(userLoader.saveUser(any(Users.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result =
                userServices.updateUser(1L, dto, "fake-token");

        assertEquals("NewName", result.getName());
        assertEquals("NewLast", result.getLastname());

        verify(userLoader).saveUser(any(Users.class));
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

        when(userLoader.findByUsername("User"))
                .thenReturn(access);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userServices.updateUser(2L, dto, "fake-token")
        );

        assertEquals(
                "You can only update your own profile",
                ex.getMessage()
        );

        verify(userLoader, never()).saveUser(any());
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

        when(userLoader.findByUsername("User"))
                .thenReturn(access);

        when(userLoader.findById(1L))
                .thenReturn(user);

        when(userLoader.saveUser(any(Users.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result =
                userServices.updateDisplayName(1L, "fake-token", dto);

        assertEquals("NewName", result.getDisplayName());

        verify(userLoader).saveUser(any(Users.class));
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

        when(userLoader.findByUsername("User"))
                .thenReturn(access);

        when(userLoader.findById(1L))
                .thenReturn(user);

        doThrow(new AlreadyExistsException("Already Existing username"))
                .when(userLoader)
                .existByDisplayName(dto);

        AlreadyExistsException exception = assertThrows(
                AlreadyExistsException.class,
                () -> userServices.updateDisplayName(1L, "fake-token", dto)
        );

        assertEquals(
                "Already Existing username",
                exception.getMessage()
        );

        verify(userLoader, never()).saveUser(any());
    }

    @Test
    void shouldUpdateUserRoleSuccessfully(){
        UserRole adminRole = new UserRole();
        adminRole.setRoleName("ADMIN");
        adminRole.setResources(List.of(Resources.UPDATE_ROLE));

        Users requester = new Users();
        requester.setId(1L);
        requester.setUserRole(adminRole);

        UserAccess access = new UserAccess();
        access.setUsername("Admin");
        access.setUser(requester);

        UserRole premiumRole = new UserRole();
        premiumRole.setRoleName("PREMIUM");

        Users targetUser = new Users();
        targetUser.setId(2L);
        targetUser.setDisplayName("Target");

        UpdateRoleDTO dto = new UpdateRoleDTO();
        dto.setRoleName("PREMIUM");

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Admin");

        when(userLoader.findByUsername("Admin"))
                .thenReturn(access);

        when(userLoader.findById(2L))
                .thenReturn(targetUser);

        when(userRoleLoader.findByRoleName("PREMIUM"))
                .thenReturn(premiumRole);

        when(userLoader.saveUser(any(Users.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result =
                userServices.updateUserRole(2L, dto, "fake-token");

        assertEquals("PREMIUM", result.getRoleName());
        assertEquals("PREMIUM", targetUser.getUserRole().getRoleName());

        verify(userLoader).saveUser(targetUser);
    }

    @Test
    void shouldThrowWhenRequesterDoesNotHaveUpdateRolePermission(){
        UserRole freeRole = new UserRole();
        freeRole.setRoleName("FREE");
        freeRole.setResources(List.of(Resources.VIEW_EVENT));

        Users requester = new Users();
        requester.setId(1L);
        requester.setUserRole(freeRole);

        UserAccess access = new UserAccess();
        access.setUsername("User");
        access.setUser(requester);

        UpdateRoleDTO dto = new UpdateRoleDTO();
        dto.setRoleName("PREMIUM");

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("User");

        when(userLoader.findByUsername("User"))
                .thenReturn(access);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> userServices.updateUserRole(2L, dto, "fake-token")
        );

        assertEquals("Only users with UPDATE_ROLE can update roles", exception.getMessage());

        verify(userLoader, never()).findById(2L);
        verify(userRoleLoader, never()).findByRoleName(any());
        verify(userLoader, never()).saveUser(any());
    }

    @Test
    void shouldThrowWhenRequesterHasNoRoleTryingToUpdateRole(){
        Users requester = new Users();
        requester.setId(1L);

        UserAccess access = new UserAccess();
        access.setUsername("User");
        access.setUser(requester);

        UpdateRoleDTO dto = new UpdateRoleDTO();
        dto.setRoleName("PREMIUM");

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("User");

        when(userLoader.findByUsername("User"))
                .thenReturn(access);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> userServices.updateUserRole(2L, dto, "fake-token")
        );

        assertEquals("Only users with UPDATE_ROLE can update roles", exception.getMessage());

        verify(userLoader, never()).findById(2L);
        verify(userRoleLoader, never()).findByRoleName(any());
        verify(userLoader, never()).saveUser(any());
    }
}
