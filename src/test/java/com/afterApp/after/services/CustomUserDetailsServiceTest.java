package com.afterApp.after.services;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccessRepository userAccessRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldLoadAuthoritiesFromUserRole() {
        UserRole role = new UserRole();
        role.setRoleName("PREMIUM");
        role.setResources(List.of(Resources.VIEW_EVENT, Resources.INVITE_USER));

        Users user = new Users();
        user.setUserRole(role);

        UserAccess access = new UserAccess();
        access.setUsername("premiumuser");
        access.setPassword("encoded-password");
        access.setUser(user);

        when(userAccessRepository.findByUsername("premiumuser")).thenReturn(Optional.of(access));

        var userDetails = customUserDetailsService.loadUserByUsername("premiumuser");

        assertEquals("premiumuser", userDetails.getUsername());
        assertEquals(
                List.of("INVITE_USER", "VIEW_EVENT"),
                userDetails.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .sorted()
                        .toList()
        );
    }

    @Test
    void shouldThrowWhenUserAccessDoesNotExist() {
        when(userAccessRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing")
        );
    }
}
