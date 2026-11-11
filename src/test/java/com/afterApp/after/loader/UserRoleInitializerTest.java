package com.afterApp.after.loader;

import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleInitializerTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRoleInitializer initializer;

    private final UserRole free = new UserRole();
    private final UserRole premium = new UserRole();
    private final UserRole admin = new UserRole();

    @BeforeEach
    void setUp() {
        when(userRoleRepository.findByRoleName("FREE")).thenReturn(Optional.of(free));
        when(userRoleRepository.findByRoleName("PREMIUM")).thenReturn(Optional.of(premium));
        when(userRoleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(admin));
        when(userRoleRepository.save(free)).thenReturn(free);
        when(userRoleRepository.save(premium)).thenReturn(premium);
        when(userRoleRepository.save(admin)).thenReturn(admin);
    }

    @Test
    void shouldGiveFreeRoleToUsersWithoutRole() {
        Users withoutRole = new Users();
        when(userRepository.findByUserRoleIsNull()).thenReturn(List.of(withoutRole));

        initializer.run();

        assertEquals(free, withoutRole.getUserRole());
        verify(userRepository).saveAll(List.of(withoutRole));
    }

    @Test
    void shouldNotLetFreeUsersInvite() {
        when(userRepository.findByUserRoleIsNull()).thenReturn(List.of());

        initializer.run();

        assertTrue(free.getResources().contains(Resources.CREATE_EVENT));
        assertTrue(free.getResources().contains(Resources.KICK_USER));
        assertFalse(free.getResources().contains(Resources.INVITE_USER));
    }

    @Test
    void shouldGivePremiumAllFreePermissionsPlusInvite() {
        when(userRepository.findByUserRoleIsNull()).thenReturn(List.of());

        initializer.run();

        assertTrue(premium.getResources().containsAll(free.getResources()));
        assertTrue(premium.getResources().contains(Resources.INVITE_USER));
    }

    @Test
    void shouldGiveAdminAllPremiumPermissionsPlusRoleAndMetrics() {
        when(userRepository.findByUserRoleIsNull()).thenReturn(List.of());

        initializer.run();

        assertTrue(admin.getResources().containsAll(premium.getResources()));
        assertTrue(admin.getResources().contains(Resources.UPDATE_ROLE));
        assertTrue(admin.getResources().contains(Resources.VIEW_METRICS));
    }
}
