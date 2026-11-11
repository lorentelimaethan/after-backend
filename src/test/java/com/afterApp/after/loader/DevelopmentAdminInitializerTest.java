package com.afterApp.after.loader;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.repositories.UserAccessRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class DevelopmentAdminInitializerTest {

    @Autowired
    private DevelopmentAdminInitializer initializer;

    @Autowired
    private UserAccessRepository userAccessRepository;

    // Read from the configuration: environment variables override application-test.properties
    @Value("${after.admin.username}")
    private String adminUsername;

    @Value("${after.admin.password}")
    private String adminPassword;

    @Value("${after.admin.email}")
    private String adminEmail;

    @Test
    void shouldCreateAdminWithAdminRole() {
        initializer.run();

        UserAccess admin = userAccessRepository.findByUsername(adminUsername).orElseThrow();
        assertEquals("ADMIN", admin.getUser().getUserRole().getRoleName());
        assertEquals(adminEmail, admin.getUser().getEmail());
    }

    @Test
    void shouldSaveAdminPasswordHashed() {
        initializer.run();

        UserAccess admin = userAccessRepository.findByUsername(adminUsername).orElseThrow();
        assertNotEquals(adminPassword, admin.getPassword());
    }

    @Test
    void shouldNotCreateAdminTwice() {
        initializer.run();
        initializer.run();

        long admins = userAccessRepository.findAll().stream()
                .filter(access -> access.getUsername().equals(adminUsername))
                .count();
        assertEquals(1, admins);
    }
}
