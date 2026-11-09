package com.afterApp.after.integration;

import com.afterApp.after.entity.Address;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.loader.EventLoader;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import com.afterApp.after.utils.TokenUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "AFTER_RUN_MYSQL_TESTS", matches = "true")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MySqlContainerTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:tc:mysql:8.0.36:///after_test");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired private MockMvc mvc;
    @Autowired private EventRepository events;
    @Autowired private UserAccessRepository accesses;
    @Autowired private UserRepository users;
    @Autowired private UserRoleRepository roles;
    @Autowired private TokenUtil tokens;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactions;
    @MockitoSpyBean private EventLoader eventLoader;

    @BeforeEach
    void setUp() {
        events.deleteAll();
        accesses.deleteAll();
        users.deleteAll();
    }

    @Test
    void shouldPersistRoleEventAddressAndAttendeeInMySql() throws Exception {
        Users host = createUser("mysql-host");
        Users guest = createUser("mysql-guest");
        Long id = createEvent(host).getId();

        mvc.perform(patch("/after/v1/events/{id}/join", id)
                        .header("Authorization", "Bearer " + tokens.generateToken("mysql-guest")))
                .andExpect(status().isOk());

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            Events loaded = events.findById(id).orElseThrow();
            assertEquals(host.getId(), loaded.getHost().getId());
            assertEquals(guest.getId(), loaded.getUsers().iterator().next().getId());
            assertEquals("Barcelona", loaded.getAddress().getCity());
            assertTrue(loaded.getHost().getUserRole().getResources().contains(Resources.VIEW_EVENT));
        });
    }

    @Test
    void shouldRemoveAnAttendeeFromAnEventWhenDeletingTheirAccountInMySql() throws Exception {
        Users host = createUser("mysql-host");
        Users guest = createUser("mysql-guest");
        Events event = createEvent(host);
        event.getUsers().add(guest);
        Long id = events.saveAndFlush(event).getId();

        mvc.perform(delete("/after/v1/users/{id}", guest.getId())
                        .header("Authorization", "Bearer " + tokens.generateToken("mysql-guest")))
                .andExpect(status().isNoContent());

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            assertEquals(0, events.findById(id).orElseThrow().getUsers().size());
        });
    }

    @Test
    void shouldNeverAdmitTwoGuestsIntoTheLastPlace() throws Exception {
        Users host = createUser("mysql-host");
        createUser("mysql-guest-one");
        createUser("mysql-guest-two");
        Events event = createEvent(host);
        event.setCapacity(1);
        Long id = events.saveAndFlush(event).getId();

        CountDownLatch loaded = new CountDownLatch(2);
        doAnswer(invocation -> {
            Events snapshot = (Events) invocation.callRealMethod();
            loaded.countDown();
            assertTrue(loaded.await(10, TimeUnit.SECONDS));
            return snapshot;
        }).when(eventLoader).getEventById(id);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> join(id, "mysql-guest-one"));
            var two = executor.submit(() -> join(id, "mysql-guest-two"));
            int firstStatus = one.get(20, TimeUnit.SECONDS);
            int secondStatus = two.get(20, TimeUnit.SECONDS);
            assertEquals(1, (firstStatus == 200 ? 1 : 0) + (secondStatus == 200 ? 1 : 0));
            assertEquals(1, (firstStatus == 400 ? 1 : 0) + (secondStatus == 400 ? 1 : 0));
        }

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            assertEquals(1, events.findById(id).orElseThrow().getUsers().size());
        });
    }

    private int join(Long id, String username) throws Exception {
        return mvc.perform(patch("/after/v1/events/{id}/join", id)
                        .header("Authorization", "Bearer " + tokens.generateToken(username)))
                .andReturn().getResponse().getStatus();
    }

    private Users createUser(String username) {
        Users user = new Users();
        user.setDisplayName(username);
        user.setUserRole(roles.findByRoleName("FREE").orElseThrow());
        UserAccess access = new UserAccess();
        access.setUsername(username);
        access.setPassword("unused");
        access.setUser(user);
        return accesses.saveAndFlush(access).getUser();
    }

    private Events createEvent(Users host) {
        Events event = new Events();
        event.setName("MySQL event");
        event.setDescription("Persistence test");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setCapacity(3);
        event.setEventType(EventType.AFTER);
        event.setMusicStyle(MusicStyle.TECHNO);
        event.setHost(host);
        Address address = new Address();
        address.setCity("Barcelona");
        event.setAddress(address);
        return events.saveAndFlush(event);
    }
}
