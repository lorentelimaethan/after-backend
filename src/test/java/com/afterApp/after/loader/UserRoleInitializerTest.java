package com.afterApp.after.loader;

import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserRoleInitializerTest {
    @Test
    void shouldInitializeRolesAndAssignOnlyUsersWithoutRoles() {
        UserRoleRepository roles = mock(UserRoleRepository.class);
        UserRepository users = mock(UserRepository.class);
        UserRole free = new UserRole();
        UserRole premium = new UserRole();
        UserRole admin = new UserRole();
        when(roles.findByRoleName("FREE")).thenReturn(Optional.of(free));
        when(roles.findByRoleName("PREMIUM")).thenReturn(Optional.of(premium));
        when(roles.findByRoleName("ADMIN")).thenReturn(Optional.of(admin));
        when(roles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Users withoutRole = new Users();
        when(users.findByUserRoleIsNull()).thenReturn(List.of(withoutRole));
        UserRoleInitializer initializer = new UserRoleInitializer(roles, users);

        initializer.run();
        initializer.run();

        assertSame(free, withoutRole.getUserRole());
        assertTrue(free.getResources().contains(Resources.CREATE_EVENT));
        assertFalse(free.getResources().contains(Resources.INVITE_USER));
        assertTrue(free.getResources().contains(Resources.KICK_USER));
        assertTrue(premium.getResources().containsAll(free.getResources()));
        assertTrue(premium.getResources().contains(Resources.INVITE_USER));
        assertTrue(admin.getResources().containsAll(premium.getResources()));
        assertTrue(admin.getResources().contains(Resources.UPDATE_ROLE));
        verify(users, times(2)).saveAll(List.of(withoutRole));
    }
}
