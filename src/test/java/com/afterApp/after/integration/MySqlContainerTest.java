package com.afterApp.after.integration;

import com.afterApp.after.support.HttpTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Same API, but with a real MySQL database started by Testcontainers (needs Docker).
 * Runs only with AFTER_RUN_MYSQL_TESTS=true (CI sets it).
 *
 * The last tests send several requests at the same time to check that the
 * capacity of an event is never exceeded.
 */
@EnabledIfEnvironmentVariable(named = "AFTER_RUN_MYSQL_TESTS", matches = "true")
class MySqlContainerTest extends HttpTestBase {

    // "jdbc:tc:mysql:..." tells Testcontainers to start a MySQL container for these tests
    @DynamicPropertySource
    static void useMySqlContainer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:tc:mysql:8.0.36:///after_test");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    // ---------- persistence ----------

    @Test
    void shouldSaveEventWithHostAndAddress() throws Exception {
        Long hostId = createUser("host").getId();

        Long eventId = createEvent(tokenFor("host"), "MySQL event", 3);

        assertEquals(hostId, eventRepository.findById(eventId).orElseThrow().getHost().getId());
        assertEquals("Barcelona", eventRepository.findById(eventId).orElseThrow().getAddress().getCity());
    }

    @Test
    void shouldRemoveAttendeeWhenTheyDeleteTheirAccount() throws Exception {
        createUser("host");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "MySQL event", 3);
        assertEquals(200, join(eventId, "guest"));

        mockMvc.perform(delete("/after/v1/users/{id}", guestId).header("Authorization", tokenFor("guest")))
                .andExpect(status().isNoContent());

        assertEquals(0, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldDeleteHostedEventWhenHostDeletesAccount() throws Exception {
        Long hostId = createUser("host").getId();
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "MySQL event", 3);
        assertEquals(200, join(eventId, "guest"));

        mockMvc.perform(delete("/after/v1/users/{id}", hostId).header("Authorization", tokenFor("host")))
                .andExpect(status().isNoContent());

        assertFalse(eventRepository.existsById(eventId));
        assertTrue(userRepository.existsById(guestId));
    }

    // ---------- requests at the same time ----------

    @Test
    void shouldAdmitOnlyOneGuestWhenManyJoinTheLastPlace() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "One place", 1);
        List<Callable<Integer>> requests = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String guest = "guest" + i;
            createUser(guest);
            requests.add(() -> join(eventId, guest));
        }

        List<Integer> statuses = sendAtTheSameTime(requests);

        assertEquals(1, count(statuses, 200));
        assertEquals(9, count(statuses, 400));
        assertEquals(1, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldAdmitOnlyOneOfTwoInvitationsForTheLastPlace() throws Exception {
        createUser("host", "PREMIUM");
        Long firstId = createUser("first").getId();
        Long secondId = createUser("second").getId();
        Long eventId = createEvent(tokenFor("host"), "One place", 1);

        List<Integer> statuses = sendAtTheSameTime(List.of(
                () -> invite(eventId, firstId),
                () -> invite(eventId, secondId)));

        assertEquals(1, count(statuses, 200));
        assertEquals(1, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldAdmitOnlyOneWhenJoinAndInvitationRaceForTheLastPlace() throws Exception {
        createUser("host", "PREMIUM");
        createUser("joining");
        Long invitedId = createUser("invited").getId();
        Long eventId = createEvent(tokenFor("host"), "One place", 1);

        List<Integer> statuses = sendAtTheSameTime(List.of(
                () -> join(eventId, "joining"),
                () -> invite(eventId, invitedId)));

        assertEquals(1, count(statuses, 200));
        assertEquals(1, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldNeverHaveMoreAttendeesThanCapacityWhenCapacityIsReducedDuringJoin() throws Exception {
        createUser("host");
        createUser("attending");
        createUser("joining");
        Long eventId = createEvent(tokenFor("host"), "Two places", 2);
        assertEquals(200, join(eventId, "attending"));

        List<Integer> statuses = sendAtTheSameTime(List.of(
                () -> updateCapacity(eventId, 1),
                () -> join(eventId, "joining")));

        assertEquals(1, count(statuses, 200));
        assertTrue(usersCount(eventId, tokenFor("host")) <= capacity(eventId));
    }

    @Test
    void shouldAlwaysRemoveLeavingGuestWhenSomeoneJoinsAtTheSameTime() throws Exception {
        createUser("host");
        Long leavingId = createUser("leaving").getId();
        createUser("joining");
        Long eventId = createEvent(tokenFor("host"), "One place", 1);
        assertEquals(200, join(eventId, "leaving"));

        List<Integer> statuses = sendAtTheSameTime(List.of(
                () -> leave(eventId, "leaving"),
                () -> join(eventId, "joining")));

        assertEquals(200, statuses.get(0));
        assertTrue(eventRepository.findDistinctByUsers_Id(leavingId).isEmpty());
        int expectedAttendees = statuses.get(1) == 200 ? 1 : 0;
        assertEquals(expectedAttendees, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldAlwaysRemoveKickedGuestWhenSomeoneJoinsAtTheSameTime() throws Exception {
        createUser("host");
        Long kickedId = createUser("kicked").getId();
        createUser("joining");
        Long eventId = createEvent(tokenFor("host"), "One place", 1);
        assertEquals(200, join(eventId, "kicked"));

        List<Integer> statuses = sendAtTheSameTime(List.of(
                () -> kick(eventId, kickedId),
                () -> join(eventId, "joining")));

        assertEquals(200, statuses.get(0));
        assertTrue(eventRepository.findDistinctByUsers_Id(kickedId).isEmpty());
        int expectedAttendees = statuses.get(1) == 200 ? 1 : 0;
        assertEquals(expectedAttendees, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldDeleteEventEvenIfSomeoneJoinsAtTheSameTime() throws Exception {
        createUser("host");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        List<Integer> statuses = sendAtTheSameTime(List.of(
                () -> deleteEvent(eventId),
                () -> join(eventId, "guest")));

        assertEquals(204, statuses.get(0));
        assertFalse(eventRepository.existsById(eventId));
        assertTrue(userRepository.existsById(guestId));
    }

    // ---------- helpers ----------

    /** Runs every request in its own thread at the same time and returns the HTTP status codes in order. */
    private List<Integer> sendAtTheSameTime(List<Callable<Integer>> requests) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        try {
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : executor.invokeAll(requests)) {
                statuses.add(result.get());
            }
            return statuses;
        } finally {
            executor.shutdown();
        }
    }

    private long count(List<Integer> statuses, int status) {
        return statuses.stream().filter(s -> s == status).count();
    }

    private int capacity(Long eventId) throws Exception {
        String response = mockMvc.perform(get("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.capacity");
    }

    private int join(Long eventId, String username) throws Exception {
        return mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor(username)))
                .andReturn().getResponse().getStatus();
    }

    private int leave(Long eventId, String username) throws Exception {
        return mockMvc.perform(patch("/after/v1/events/{id}/leave", eventId)
                        .header("Authorization", tokenFor(username)))
                .andReturn().getResponse().getStatus();
    }

    private int invite(Long eventId, Long userId) throws Exception {
        return mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{userId}", eventId, userId)
                        .header("Authorization", tokenFor("host")))
                .andReturn().getResponse().getStatus();
    }

    private int kick(Long eventId, Long userId) throws Exception {
        return mockMvc.perform(delete("/after/v1/events/{id}/kick/user/{userId}", eventId, userId)
                        .header("Authorization", tokenFor("host")))
                .andReturn().getResponse().getStatus();
    }

    private int updateCapacity(Long eventId, int capacity) throws Exception {
        return mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\": " + capacity + "}"))
                .andReturn().getResponse().getStatus();
    }

    private int deleteEvent(Long eventId) throws Exception {
        return mockMvc.perform(delete("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andReturn().getResponse().getStatus();
    }
}
