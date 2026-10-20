package com.afterApp.after.loader;

import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserRoleInitializerTest {
    @Test
    void shouldCreateAdminOnceWithConfiguredProfileAndAdminRole() {
        UserRoleRepository roles = mock(UserRoleRepository.class);
        UserRepository users = mock(UserRepository.class);
        UserAccessRepository accesses = mock(UserAccessRepository.class);
        when(roles.findByRoleName(anyString())).thenReturn(Optional.empty());
        when(roles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findByUserRoleIsNull()).thenReturn(List.of());
        when(accesses.existsByUsername("admin")).thenReturn(false, true);
        UserRoleInitializer initializer = new UserRoleInitializer(roles, accesses, users);
        ReflectionTestUtils.setField(initializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "adminPassword", "secret123");
        ReflectionTestUtils.setField(initializer, "adminName", "Name");
        ReflectionTestUtils.setField(initializer, "adminLastname", "Lastname");
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(initializer, "adminPhoneNumber", "123456789");

        initializer.run();
        initializer.run();

        ArgumentCaptor<UserAccess> access = ArgumentCaptor.forClass(UserAccess.class);
        verify(accesses, times(1)).save(access.capture());
        assertEquals("admin", access.getValue().getUsername());
        assertNotEquals("secret123", access.getValue().getPassword());
        assertEquals("ADMIN", access.getValue().getUser().getUserRole().getRoleName());
        assertEquals("admin@example.com", access.getValue().getUser().getEmail());
        assertEquals("Lastname", access.getValue().getUser().getLastname());
        assertEquals("123456789", access.getValue().getUser().getPhoneNumber());
    }

    @Test
    void shouldInitializeRolesAndAssignOnlyUsersWithoutRoles() {
        UserRoleRepository roles = mock(UserRoleRepository.class);
        UserRepository users = mock(UserRepository.class);
        UserAccessRepository accesses = mock(UserAccessRepository.class);
        UserRole free = new UserRole();
        UserRole premium = new UserRole();
        UserRole admin = new UserRole();
        when(roles.findByRoleName("FREE")).thenReturn(Optional.of(free));
        when(roles.findByRoleName("PREMIUM")).thenReturn(Optional.of(premium));
        when(roles.findByRoleName("ADMIN")).thenReturn(Optional.of(admin));
        when(roles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Users withoutRole = new Users();
        when(users.findByUserRoleIsNull()).thenReturn(List.of(withoutRole));
        when(accesses.existsByUsername("admin")).thenReturn(true);
        UserRoleInitializer initializer = new UserRoleInitializer(roles, accesses, users);
        ReflectionTestUtils.setField(initializer, "adminUsername", "admin");

        initializer.run();
        initializer.run();

        assertSame(free, withoutRole.getUserRole());
        assertTrue(free.getResources().contains(Resources.CREATE_EVENT));
        assertFalse(free.getResources().contains(Resources.INVITE_USER));
        assertTrue(premium.getResources().containsAll(free.getResources()));
        assertTrue(premium.getResources().contains(Resources.INVITE_USER));
        assertTrue(admin.getResources().containsAll(premium.getResources()));
        assertTrue(admin.getResources().contains(Resources.UPDATE_ROLE));
        verify(users, times(2)).saveAll(List.of(withoutRole));
        verify(accesses, never()).save(any());
    }
}
