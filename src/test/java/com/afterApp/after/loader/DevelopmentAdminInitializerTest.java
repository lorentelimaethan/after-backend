package com.afterApp.after.loader;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevelopmentAdminInitializerTest {

    @Test
    void shouldCreateAdminOnceWithConfiguredProfileAndAdminRole() {
        UserRoleRepository roles = mock(UserRoleRepository.class);
        UserAccessRepository accesses = mock(UserAccessRepository.class);
        UserRole adminRole = new UserRole();

        when(roles.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(accesses.existsByUsername("admin")).thenReturn(false, true);

        DevelopmentAdminInitializer initializer = new DevelopmentAdminInitializer(roles, accesses);
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
        assertEquals(adminRole, access.getValue().getUser().getUserRole());
        assertEquals("admin@example.com", access.getValue().getUser().getEmail());
        assertEquals("Lastname", access.getValue().getUser().getLastname());
        assertEquals("123456789", access.getValue().getUser().getPhoneNumber());
    }
}
