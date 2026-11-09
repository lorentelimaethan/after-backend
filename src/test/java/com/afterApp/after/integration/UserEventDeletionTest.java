package com.afterApp.after.integration;

import com.afterApp.after.entity.Address;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import com.afterApp.after.utils.TokenUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserEventDeletionTest {
    @Autowired private MockMvc mvc;
    @Autowired private EventRepository events;
    @Autowired private UserAccessRepository accesses;
    @Autowired private UserRepository users;
    @Autowired private UserRoleRepository roles;
    @Autowired private TokenUtil tokens;
    @Autowired private TransactionTemplate transactions;
    @Autowired private EntityManager entityManager;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        events.deleteAll();
        accesses.deleteAll();
        users.deleteAll();
    }

    @Test
    void shouldRemoveDeletedAttendeeWithoutDeletingTheEvent() throws Exception {
        Users host = createUser("host");
        Users guest = createUser("guest");
        Events event = createEvent(host);
        event.getUsers().add(guest);
        events.saveAndFlush(event);

        mvc.perform(delete("/after/v1/users/{id}", guest.getId())
                        .header("Authorization", "Bearer " + tokens.generateToken("guest")))
                .andExpect(status().isNoContent());

        assertTrue(users.findById(guest.getId()).isEmpty());
        assertTrue(accesses.findByUsername("guest").isEmpty());
        assertTrue(users.existsById(host.getId()));
        assertTrue(events.existsById(event.getId()));
        transactions.executeWithoutResult(status -> assertEquals(0,
                events.findById(event.getId()).orElseThrow().getUsers().size()));
    }

    @Test
    void shouldDeleteHostedEventWithoutDeletingItsAttendees() throws Exception {
        Users host = createUser("host");
        Users guest = createUser("guest");
        Events event = createEvent(host);
        event.getUsers().add(guest);
        events.saveAndFlush(event);
        Long addressId = event.getAddress().getId();

        mvc.perform(delete("/after/v1/users/{id}", host.getId())
                        .header("Authorization", "Bearer " + tokens.generateToken("host")))
                .andExpect(status().isNoContent());

        assertFalse(users.existsById(host.getId()));
        assertTrue(accesses.findByUsername("host").isEmpty());
        assertFalse(events.existsById(event.getId()));
        assertTrue(users.existsById(guest.getId()));
        assertTrue(accesses.findByUsername("guest").isPresent());
        assertNull(transactions.execute(status -> entityManager.find(Address.class, addressId)));
    }

    @Test
    void shouldRemoveAttendanceAndHostedEventsInTheSameAccountDeletion() throws Exception {
        Users target = createUser("target");
        Users otherHost = createUser("other-host");
        Users hostedGuest = createUser("hosted-guest");
        Users otherGuest = createUser("other-guest");

        Events hosted = createEvent(target);
        hosted.getUsers().add(hostedGuest);
        events.saveAndFlush(hosted);
        Events attended = createEvent(otherHost);
        attended.getUsers().add(target);
        attended.getUsers().add(otherGuest);
        events.saveAndFlush(attended);

        cacheManager.getCache("users").put(target.getId(), "stale user");
        cacheManager.getCache("events").put(hosted.getId(), "stale event");
        cacheManager.getCache("events").put(attended.getId(), "stale attendee count");
        cacheManager.getCache("eventsByType").put(EventType.AFTER, "stale list");
        cacheManager.getCache("eventsByStyle").put(MusicStyle.TECHNO, "stale list");
        cacheManager.getCache("eventsByTypeStyle").put("AFTER-TECHNO", "stale list");

        mvc.perform(delete("/after/v1/users/{id}", target.getId())
                        .header("Authorization", "Bearer " + tokens.generateToken("target")))
                .andExpect(status().isNoContent());

        assertFalse(users.existsById(target.getId()));
        assertFalse(events.existsById(hosted.getId()));
        assertTrue(users.existsById(hostedGuest.getId()));
        assertTrue(users.existsById(otherHost.getId()));
        assertTrue(users.existsById(otherGuest.getId()));
        transactions.executeWithoutResult(status -> {
            Events remaining = events.findById(attended.getId()).orElseThrow();
            assertEquals(otherHost.getId(), remaining.getHost().getId());
            assertEquals(1, remaining.getUsers().size());
            assertEquals(otherGuest.getId(), remaining.getUsers().iterator().next().getId());
        });
        assertNull(cacheManager.getCache("users").get(target.getId()));
        assertNull(cacheManager.getCache("events").get(hosted.getId()));
        assertNull(cacheManager.getCache("events").get(attended.getId()));
        assertNull(cacheManager.getCache("eventsByType").get(EventType.AFTER));
        assertNull(cacheManager.getCache("eventsByStyle").get(MusicStyle.TECHNO));
        assertNull(cacheManager.getCache("eventsByTypeStyle").get("AFTER-TECHNO"));
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
        event.setName("Deletion test");
        event.setDescription("Deletion test");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setCapacity(2);
        event.setEventType(EventType.AFTER);
        event.setMusicStyle(MusicStyle.TECHNO);
        event.setAddress(new Address());
        event.setHost(host);
        return events.saveAndFlush(event);
    }
}
