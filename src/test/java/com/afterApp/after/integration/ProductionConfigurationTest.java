package com.afterApp.after.integration;

import com.afterApp.after.loader.DevelopmentAdminInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Starts the application with the prod profile (using the in-memory test database). */
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

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldNotCreateDevelopmentAdminInProd() {
        assertTrue(context.getBeansOfType(DevelopmentAdminInitializer.class).isEmpty());
    }

    @Test
    void shouldReadJwtSecretFromEnvironmentInProd() throws IOException {
        Properties prod = loadProdProperties();

        assertEquals("${JWT_SECRET}", prod.getProperty("jwt.secret"));
    }

    @Test
    void shouldReadDatabaseCredentialsFromEnvironmentInProd() throws IOException {
        Properties prod = loadProdProperties();

        assertEquals("${DATABASE_URL}", prod.getProperty("spring.datasource.url"));
        assertEquals("${DATABASE_USERNAME}", prod.getProperty("spring.datasource.username"));
        assertEquals("${DATABASE_PASSWORD}", prod.getProperty("spring.datasource.password"));
    }

    /** Reads application-prod.properties as plain text, without resolving the ${...} values. */
    private Properties loadProdProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream file = getClass().getResourceAsStream("/application-prod.properties")) {
            properties.load(file);
        }
        return properties;
    }
}
