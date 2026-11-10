package com.afterApp.after.integration;

import com.afterApp.after.loader.DevelopmentAdminInitializer;
import com.afterApp.after.repositories.UserAccessRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:hsqldb:mem:prod_profile_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
class ProductionConfigurationTest {
    @Autowired private ApplicationContext context;
    @Autowired private UserAccessRepository accesses;

    @Test
    void shouldStartProdWithoutDevelopmentAdmin() {
        assertTrue(context.getBeansOfType(DevelopmentAdminInitializer.class).isEmpty());
        assertTrue(accesses.findByUsername("adminTest").isEmpty());
    }

    @Test
    void shouldRequireJwtSecretInProductionProperties() throws IOException {
        var environment = productionProperties();
        assertEquals("${JWT_SECRET}", environment.getPropertySources().get("production").getProperty("jwt.secret"));
        assertThrows(IllegalArgumentException.class, () -> environment.getProperty("jwt.secret"));
    }

    @Test
    void shouldRequireDatabaseCredentialsInProductionProperties() throws IOException {
        var environment = productionProperties();
        for (String name : new String[] {"spring.datasource.url", "spring.datasource.username", "spring.datasource.password"}) {
            assertThrows(IllegalArgumentException.class, () -> environment.getProperty(name));
        }
    }

    private MockEnvironment productionProperties() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new ResourcePropertySource("production", "classpath:application-prod.properties"));
        return environment;
    }
}
