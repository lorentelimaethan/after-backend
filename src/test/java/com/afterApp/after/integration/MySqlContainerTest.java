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
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
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
    void shouldDeleteHostedEventButKeepAttendeeAccountInMySql() throws Exception {
        Users host = createUser("mysql-host");
        Users guest = createUser("mysql-guest");
        Events event = createEvent(host);
        event.getUsers().add(guest);
        Long id = events.saveAndFlush(event).getId();

        mvc.perform(delete("/after/v1/users/{id}", host.getId())
                        .header("Authorization", "Bearer " + tokens.generateToken("mysql-host")))
                .andExpect(status().isNoContent());

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            assertTrue(users.findById(host.getId()).isEmpty());
            assertTrue(events.findById(id).isEmpty());
            assertTrue(users.findById(guest.getId()).isPresent());
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

        assertOneSuccessAndOneFull(runConcurrently(id,
                () -> join(id, "mysql-guest-one"),
                () -> join(id, "mysql-guest-two")));

        assertAttendeeCount(id, 1);
    }

    @Test
    void shouldNeverAdmitTwoInvitationsIntoTheLastPlace() throws Exception {
        Users host = createUser("mysql-host", "PREMIUM");
        Users firstGuest = createUser("mysql-guest-one");
        Users secondGuest = createUser("mysql-guest-two");
        Events event = createEvent(host);
        event.setCapacity(1);
        Long id = events.saveAndFlush(event).getId();

        assertOneSuccessAndOneFull(runConcurrently(id,
                () -> invite(id, firstGuest.getId()),
                () -> invite(id, secondGuest.getId())));
        assertAttendeeCount(id, 1);
    }

    @Test
    void shouldNeverAdmitJoinAndInvitationIntoTheLastPlace() throws Exception {
        Users host = createUser("mysql-host", "PREMIUM");
        createUser("mysql-joining-guest");
        Users invited = createUser("mysql-invited-guest");
        Events event = createEvent(host);
        event.setCapacity(1);
        Long id = events.saveAndFlush(event).getId();

        assertOneSuccessAndOneFull(runConcurrently(id,
                () -> join(id, "mysql-joining-guest"),
                () -> invite(id, invited.getId())));
        assertAttendeeCount(id, 1);
    }

    @Test
    void shouldKeepCapacityValidWhenReductionRacesWithJoin() throws Exception {
        Users host = createUser("mysql-host");
        Users attending = createUser("mysql-attending-guest");
        createUser("mysql-joining-guest");
        Events event = createEvent(host);
        event.setCapacity(2);
        event.getUsers().add(attending);
        Long id = events.saveAndFlush(event).getId();

        List<Integer> results = runConcurrently(id,
                () -> updateCapacity(id, 1),
                () -> join(id, "mysql-joining-guest"));
        assertOneSuccessAndOneFull(results);

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            Events loaded = events.findById(id).orElseThrow();
            assertEquals(results.get(0) == 200 ? 1 : 2, loaded.getCapacity());
            assertEquals(loaded.getCapacity(), loaded.getUsers().size());
        });
    }

    @Test
    void shouldPreserveAttendanceWhenLeaveRacesWithJoin() throws Exception {
        Users host = createUser("mysql-host");
        Users leaving = createUser("mysql-leaving-guest");
        Users joining = createUser("mysql-joining-guest");
        Events event = createEvent(host);
        event.setCapacity(1);
        event.getUsers().add(leaving);
        Long id = events.saveAndFlush(event).getId();

        List<Integer> results = runConcurrently(id,
                () -> leave(id, "mysql-leaving-guest"),
                () -> join(id, "mysql-joining-guest"));
        assertEquals(200, results.get(0));
        assertTrue(results.get(1) == 200 || results.get(1) == 400);
        assertDepartureAndPossibleJoin(id, leaving.getId(), joining.getId(), results.get(1));
    }

    @Test
    void shouldPreserveAttendanceWhenKickRacesWithJoin() throws Exception {
        Users host = createUser("mysql-host");
        Users kicked = createUser("mysql-kicked-guest");
        Users joining = createUser("mysql-joining-guest");
        Events event = createEvent(host);
        event.setCapacity(1);
        event.getUsers().add(kicked);
        Long id = events.saveAndFlush(event).getId();

        List<Integer> results = runConcurrently(id,
                () -> kick(id, kicked.getId()),
                () -> join(id, "mysql-joining-guest"));
        assertEquals(200, results.get(0));
        assertTrue(results.get(1) == 200 || results.get(1) == 400);
        assertDepartureAndPossibleJoin(id, kicked.getId(), joining.getId(), results.get(1));
    }

    private List<Integer> runConcurrently(Long id, Callable<Integer> first, Callable<Integer> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        doAnswer(invocation -> {
            ready.countDown();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            return invocation.callRealMethod();
        }).when(eventLoader).getEventForUpdate(id);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(first);
            var two = executor.submit(second);
            return List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        }
    }

    private void assertOneSuccessAndOneFull(List<Integer> results) {
        assertEquals(1, results.stream().filter(status -> status == 200).count());
        assertEquals(1, results.stream().filter(status -> status == 400).count());
    }

    private void assertAttendeeCount(Long id, int expected) {
        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            assertEquals(expected, events.findById(id).orElseThrow().getUsers().size());
        });
    }

    private void assertDepartureAndPossibleJoin(Long id, Long departedId, Long joiningId, int joinStatus) {
        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            Events loaded = events.findById(id).orElseThrow();
            assertTrue(loaded.getUsers().stream().noneMatch(user -> user.getId().equals(departedId)));
            assertEquals(joinStatus == 200 ? 1 : 0, loaded.getUsers().size());
            assertEquals(joinStatus == 200,
                    loaded.getUsers().stream().anyMatch(user -> user.getId().equals(joiningId)));
        });
    }

    private int join(Long id, String username) throws Exception {
        return mvc.perform(patch("/after/v1/events/{id}/join", id)
                        .header("Authorization", "Bearer " + tokens.generateToken(username)))
                .andReturn().getResponse().getStatus();
    }

    private int invite(Long id, Long userId) throws Exception {
        return mvc.perform(patch("/after/v1/events/{id}/invite/user/{userId}", id, userId)
                        .header("Authorization", "Bearer " + tokens.generateToken("mysql-host")))
                .andReturn().getResponse().getStatus();
    }

    private int updateCapacity(Long id, int capacity) throws Exception {
        return mvc.perform(patch("/after/v1/events/{id}", id)
                        .header("Authorization", "Bearer " + tokens.generateToken("mysql-host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\":" + capacity + "}"))
                .andReturn().getResponse().getStatus();
    }

    private int leave(Long id, String username) throws Exception {
        return mvc.perform(patch("/after/v1/events/{id}/leave", id)
                        .header("Authorization", "Bearer " + tokens.generateToken(username)))
                .andReturn().getResponse().getStatus();
    }

    private int kick(Long id, Long userId) throws Exception {
        return mvc.perform(delete("/after/v1/events/{id}/kick/user/{userId}", id, userId)
                        .header("Authorization", "Bearer " + tokens.generateToken("mysql-host")))
                .andReturn().getResponse().getStatus();
    }

    private Users createUser(String username) {
        return createUser(username, "FREE");
    }

    private Users createUser(String username, String roleName) {
        Users user = new Users();
        user.setDisplayName(username);
        user.setUserRole(roles.findByRoleName(roleName).orElseThrow());
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
