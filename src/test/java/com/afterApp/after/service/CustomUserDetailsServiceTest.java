package com.afterApp.after.service;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserAccessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccessRepository userAccessRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldLoadUsernameAndPassword() {
        when(userAccessRepository.findByUsername("premiumuser")).thenReturn(Optional.of(premiumAccess()));

        UserDetails result = customUserDetailsService.loadUserByUsername("premiumuser");

        assertEquals("premiumuser", result.getUsername());
        assertEquals("encoded-password", result.getPassword());
    }

    @Test
    void shouldLoadRoleResourcesAsAuthorities() {
        when(userAccessRepository.findByUsername("premiumuser")).thenReturn(Optional.of(premiumAccess()));

        UserDetails result = customUserDetailsService.loadUserByUsername("premiumuser");

        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("VIEW_EVENT")));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("INVITE_USER")));
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(userAccessRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing"));
    }

    private UserAccess premiumAccess() {
        UserRole role = new UserRole();
        role.setRoleName("PREMIUM");
        role.setResources(List.of(Resources.VIEW_EVENT, Resources.INVITE_USER));

        Users user = new Users();
        user.setUserRole(role);

        UserAccess access = new UserAccess();
        access.setUsername("premiumuser");
        access.setPassword("encoded-password");
        access.setUser(user);
        return access;
    }
}
