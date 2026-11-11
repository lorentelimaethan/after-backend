package com.afterApp.after.service;

import com.afterApp.after.dto.UpdateDisplayNameDTO;
import com.afterApp.after.dto.UpdateRoleDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.dto.UserPrivateResponseDTO;
import com.afterApp.after.dto.UserPublicResponseDTO;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.loader.EventLoader;
import com.afterApp.after.loader.UserLoader;
import com.afterApp.after.loader.UserRoleLoader;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServicesTest {

    private static final String TOKEN = "Bearer token";

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private UserLoader userLoader;

    @Mock
    private UserRoleLoader userRoleLoader;

    @Mock
    private EventLoader eventLoader;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UserServices userServices;

    private final Users user = user(1L, "user");

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ---------- getUserById ----------

    @Test
    void shouldReturnPrivateProfileWhenUserRequestsOwnProfile() {
        loggedInAs(user);
        user.setEmail("user@example.com");
        when(userLoader.findById(1L)).thenReturn(user);

        UserPublicResponseDTO result = userServices.getUserById(1L, TOKEN);

        assertTrue(result instanceof UserPrivateResponseDTO);
        assertEquals("user@example.com", ((UserPrivateResponseDTO) result).getEmail());
    }

    @Test
    void shouldReturnPublicProfileWhenUserRequestsAnotherProfile() {
        loggedInAs(user);
        Users other = user(2L, "other");
        when(userLoader.findById(2L)).thenReturn(other);

        UserPublicResponseDTO result = userServices.getUserById(2L, TOKEN);

        assertEquals("other", result.getDisplayName());
        assertFalse(result instanceof UserPrivateResponseDTO);
    }

    // ---------- updateUser ----------

    @Test
    void shouldUpdateOwnProfile() {
        loggedInAs(user);
        when(userLoader.findById(1L)).thenReturn(user);
        when(userLoader.saveUser(user)).thenReturn(user);
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("NewName");
        dto.setLastname("NewLast");

        UserPrivateResponseDTO result = userServices.updateUser(1L, dto, TOKEN);

        assertEquals("NewName", result.getName());
        assertEquals("NewLast", result.getLastname());
    }

    @Test
    void shouldThrowWhenUpdatingAnotherProfile() {
        loggedInAs(user);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userServices.updateUser(2L, new UpdateUserDTO(), TOKEN));

        assertEquals("You can only update your own profile", exception.getMessage());
        verify(userLoader, never()).saveUser(any());
    }

    // ---------- updateDisplayName ----------

    @Test
    void shouldChangeDisplayName() {
        loggedInAs(user);
        when(userLoader.findById(1L)).thenReturn(user);
        when(userLoader.saveUser(user)).thenReturn(user);

        UserPrivateResponseDTO result = userServices.updateDisplayName(1L, TOKEN, displayName("newName"));

        assertEquals("newName", result.getDisplayName());
    }

    @Test
    void shouldThrowWhenDisplayNameAlreadyExists() {
        loggedInAs(user);
        when(userLoader.findById(1L)).thenReturn(user);
        UpdateDisplayNameDTO dto = displayName("takenName");
        doThrow(new AlreadyExistsException("Already Existing username")).when(userLoader).existByDisplayName(dto);

        AlreadyExistsException exception = assertThrows(AlreadyExistsException.class,
                () -> userServices.updateDisplayName(1L, TOKEN, dto));

        assertEquals("Already Existing username", exception.getMessage());
        verify(userLoader, never()).saveUser(any());
    }

    @Test
    void shouldThrowWhenChangingDisplayNameOfAnotherUser() {
        loggedInAs(user);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userServices.updateDisplayName(2L, TOKEN, displayName("newName")));

        assertEquals("You can only update your own profile", exception.getMessage());
        verify(userLoader, never()).saveUser(any());
    }

    // ---------- updateUserRole ----------

    @Test
    void shouldUpdateRoleWhenRequesterHasUpdateRolePermission() {
        user.setUserRole(role("ADMIN", Resources.UPDATE_ROLE));
        loggedInAs(user);
        Users target = user(2L, "target");
        UserRole premium = role("PREMIUM");
        when(userLoader.findById(2L)).thenReturn(target);
        when(userRoleLoader.findByRoleName("PREMIUM")).thenReturn(premium);
        when(userLoader.saveUser(target)).thenReturn(target);

        UserPrivateResponseDTO result = userServices.updateUserRole(2L, roleDto("PREMIUM"), TOKEN);

        assertEquals("PREMIUM", result.getRoleName());
    }

    @Test
    void shouldThrowWhenRequesterHasNoUpdateRolePermission() {
        user.setUserRole(role("FREE", Resources.VIEW_EVENT));
        loggedInAs(user);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userServices.updateUserRole(2L, roleDto("PREMIUM"), TOKEN));

        assertEquals("Only users with UPDATE_ROLE can update roles", exception.getMessage());
        verify(userLoader, never()).saveUser(any());
    }

    @Test
    void shouldThrowWhenRequesterHasNoRole() {
        loggedInAs(user);

        assertThrows(UnauthorizedException.class,
                () -> userServices.updateUserRole(2L, roleDto("PREMIUM"), TOKEN));

        verify(userLoader, never()).saveUser(any());
    }

    // ---------- deleteUser ----------

    @Test
    void shouldDeleteOwnAccount() {
        // deleteUser registers a callback that runs after the commit,
        // so the test needs an active transaction synchronization
        TransactionSynchronizationManager.initSynchronization();
        loggedInAs(user);
        when(userLoader.findById(1L)).thenReturn(user);
        when(eventLoader.findAttendedByUserId(1L)).thenReturn(List.of());
        when(eventLoader.findHostedByUserId(1L)).thenReturn(List.of());

        userServices.deleteUser(1L, TOKEN);

        verify(userLoader).deleteUser(user);
    }

    @Test
    void shouldRemoveUserFromAttendedEventsWhenDeletingAccount() {
        TransactionSynchronizationManager.initSynchronization();
        loggedInAs(user);
        Events attended = new Events();
        attended.getUsers().add(user);
        when(userLoader.findById(1L)).thenReturn(user);
        when(eventLoader.findAttendedByUserId(1L)).thenReturn(List.of(attended));
        when(eventLoader.findHostedByUserId(1L)).thenReturn(List.of());

        userServices.deleteUser(1L, TOKEN);

        assertEquals(0, attended.getUsers().size());
        verify(eventLoader).saveAllAndFlush(List.of(attended));
    }

    @Test
    void shouldDeleteHostedEventsWhenDeletingAccount() {
        TransactionSynchronizationManager.initSynchronization();
        loggedInAs(user);
        Events hosted = new Events();
        when(userLoader.findById(1L)).thenReturn(user);
        when(eventLoader.findAttendedByUserId(1L)).thenReturn(List.of());
        when(eventLoader.findHostedByUserId(1L)).thenReturn(List.of(hosted));

        userServices.deleteUser(1L, TOKEN);

        verify(eventLoader).deleteAllAndFlush(List.of(hosted));
    }

    @Test
    void shouldThrowWhenDeletingAnotherAccount() {
        loggedInAs(user);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userServices.deleteUser(2L, TOKEN));

        assertEquals("You can only delete your own profile", exception.getMessage());
        verify(userLoader, never()).deleteUser(any());
    }

    // ---------- helpers ----------

    private void loggedInAs(Users loggedUser) {
        UserAccess access = new UserAccess();
        access.setUsername(loggedUser.getDisplayName());
        access.setUser(loggedUser);

        when(tokenUtil.extractUsername("token")).thenReturn(loggedUser.getDisplayName());
        when(userLoader.findByUsername(loggedUser.getDisplayName())).thenReturn(access);
    }

    private Users user(Long id, String displayName) {
        Users newUser = new Users();
        newUser.setId(id);
        newUser.setDisplayName(displayName);
        return newUser;
    }

    private UserRole role(String name, Resources... resources) {
        UserRole role = new UserRole();
        role.setRoleName(name);
        role.setResources(List.of(resources));
        return role;
    }

    private UpdateRoleDTO roleDto(String roleName) {
        UpdateRoleDTO dto = new UpdateRoleDTO();
        dto.setRoleName(roleName);
        return dto;
    }

    private UpdateDisplayNameDTO displayName(String name) {
        UpdateDisplayNameDTO dto = new UpdateDisplayNameDTO();
        dto.setDisplayName(name);
        return dto;
    }
}
