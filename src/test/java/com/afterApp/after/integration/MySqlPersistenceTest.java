package com.afterApp.after.integration;

import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AFTER_MYSQL_TEST_URL", matches = "jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/after_test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MySqlPersistenceTest {
    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("AFTER_MYSQL_TEST_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("AFTER_MYSQL_TEST_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("AFTER_MYSQL_TEST_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired private UserRoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private EntityManager entityManager;

    @Test
    @Transactional
    void shouldPersistUserRoleAndReadEnumResources() {
        UserRole free = roles.findByRoleName("FREE").orElseThrow();
        Users user = new Users();
        user.setDisplayName("mysql-test-user");
        user.setUserRole(free);
        Long id = users.saveAndFlush(user).getId();
        entityManager.clear();
        Users reloaded = users.findById(id).orElseThrow();
        assertEquals("FREE", reloaded.getUserRole().getRoleName());
        assertTrue(reloaded.getUserRole().getResources().contains(Resources.VIEW_EVENT));
    }
}
