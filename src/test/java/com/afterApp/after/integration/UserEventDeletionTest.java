package com.afterApp.after.integration;

import com.afterApp.after.entity.Address;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.support.HttpTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What happens to events when a user deletes their account. */
class UserEventDeletionTest extends HttpTestBase {

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldRemoveDeletedAttendeeButKeepTheEvent() throws Exception {
        createUser("host");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        join("guest", eventId);

        deleteAccount("guest", guestId);

        assertFalse(userRepository.existsById(guestId));
        assertTrue(userAccessRepository.findByUsername("guest").isEmpty());
        assertTrue(eventRepository.existsById(eventId));
        assertEquals(0, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldDeleteHostedEventButKeepItsAttendees() throws Exception {
        Long hostId = createUser("host").getId();
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        join("guest", eventId);
        Long addressId = eventRepository.findById(eventId).orElseThrow().getAddress().getId();

        deleteAccount("host", hostId);

        assertFalse(userRepository.existsById(hostId));
        assertFalse(eventRepository.existsById(eventId));
        assertNull(entityManager.find(Address.class, addressId));
        assertTrue(userRepository.existsById(guestId));
    }

    @Test
    void shouldHandleHostedAndAttendedEventsInTheSameDeletion() throws Exception {
        Long targetId = createUser("target").getId();
        createUser("other-host");
        createUser("other-guest");
        Long hostedId = createEvent(tokenFor("target"), "Hosted by target", 3);
        Long attendedId = createEvent(tokenFor("other-host"), "Attended by target", 3);
        join("target", attendedId);
        join("other-guest", attendedId);

        deleteAccount("target", targetId);

        assertFalse(eventRepository.existsById(hostedId));
        assertTrue(eventRepository.existsById(attendedId));
        assertEquals(1, usersCount(attendedId, tokenFor("other-host")));
    }

    @Test
    void shouldClearCachesAfterDeletingAccount() throws Exception {
        Long targetId = createUser("target").getId();
        cacheManager.getCache("users").put(targetId, "old user");
        cacheManager.getCache("eventsByType").put(EventType.AFTER, "old list");

        deleteAccount("target", targetId);

        assertNull(cacheManager.getCache("users").get(targetId));
        assertNull(cacheManager.getCache("eventsByType").get(EventType.AFTER));
    }

    private void join(String username, Long eventId) throws Exception {
        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor(username)))
                .andExpect(status().isOk());
    }

    private void deleteAccount(String username, Long userId) throws Exception {
        mockMvc.perform(delete("/after/v1/users/{id}", userId)
                        .header("Authorization", tokenFor(username)))
                .andExpect(status().isNoContent());
    }
}
