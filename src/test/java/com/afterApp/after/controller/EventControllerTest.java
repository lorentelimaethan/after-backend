package com.afterApp.after.controller;

import com.afterApp.after.entity.Address;
import com.afterApp.after.support.HttpTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest extends HttpTestBase {

    @Autowired
    private EntityManager entityManager;

    // ---------- GET ----------

    @Test
    void shouldGetEventById() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "After Barcelona", 3);

        mockMvc.perform(get("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.name").value("After Barcelona"))
                .andExpect(jsonPath("$.hostDisplayName").value("host"))
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        createUser("host");

        mockMvc.perform(get("/after/v1/events/{id}", 999L)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllEvents() throws Exception {
        createUser("host");
        createEvent(tokenFor("host"), "After Barcelona", 3);
        createEvent(tokenFor("host"), "House Party", "HOUSE_PARTY", "HOUSE", 4);

        mockMvc.perform(get("/after/v1/events")
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldFilterEventsByTypeAndStyle() throws Exception {
        createUser("host");
        createEvent(tokenFor("host"), "After Barcelona", "AFTER", "TECHNO", 3);
        createEvent(tokenFor("host"), "Pool Pop", "POOL_PARTY", "POP", 4);

        mockMvc.perform(get("/after/v1/events")
                        .param("type", "AFTER")
                        .param("style", "TECHNO")
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("After Barcelona"));
    }

    @Test
    void shouldRejectGetEventsWithInvalidToken() throws Exception {
        mockMvc.perform(get("/after/v1/events")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    // ---------- POST ----------

    @Test
    void shouldCreateEventWithLoggedUserAsHost() throws Exception {
        createUser("host");

        mockMvc.perform(post("/after/v1/events")
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("After Barcelona", "AFTER", "TECHNO", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After Barcelona"))
                .andExpect(jsonPath("$.hostDisplayName").value("host"));
    }

    @Test
    void shouldRejectCreateEventWithZeroCapacity() throws Exception {
        createUser("host");

        mockMvc.perform(post("/after/v1/events")
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Bad Event", "AFTER", "TECHNO", 0)))
                .andExpect(status().isBadRequest());
    }

    // ---------- join / leave ----------

    @Test
    void shouldJoinEvent() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(1));
    }

    @Test
    void shouldLeaveEvent() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        joinAs("guest", eventId);

        mockMvc.perform(patch("/after/v1/events/{id}/leave", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldRejectHostJoiningOwnEvent() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectJoiningTwice() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        joinAs("guest", eventId);

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectJoinWhenEventIsFull() throws Exception {
        createUser("host");
        createUser("first");
        createUser("second");
        Long eventId = createEvent(tokenFor("host"), "Small After", 1);
        joinAs("first", eventId);

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor("second")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Event capacity is full"));
    }

    @Test
    void shouldRejectHostLeavingOwnEvent() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}/leave", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenLeavingEventNotJoined() throws Exception {
        createUser("host");
        createUser("stranger");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}/leave", eventId)
                        .header("Authorization", tokenFor("stranger")))
                .andExpect(status().isNotFound());
    }

    // ---------- invite ----------

    @Test
    void shouldInviteUserWhenHostIsPremium() throws Exception {
        createUser("host", "PREMIUM");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(1));
    }

    @Test
    void shouldRejectInviteWhenHostIsFree() throws Exception {
        createUser("host", "FREE");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isForbidden());

        assertEquals(0, usersCount(eventId, tokenFor("host")));
    }

    @Test
    void shouldRejectInviteByNonHost() throws Exception {
        createUser("host");
        createUser("guest", "PREMIUM");
        Long targetId = createUser("target").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, targetId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only host can invite Users"));
    }

    @Test
    void shouldRejectInvitingHost() throws Exception {
        Long hostId = createUser("host", "PREMIUM").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, hostId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvitingUserAlreadyAttending() throws Exception {
        createUser("host", "PREMIUM");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        joinAs("guest", eventId);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isBadRequest());
    }

    // ---------- kick ----------

    @Test
    void shouldKickUserWhenHostIsFree() throws Exception {
        createUser("host", "FREE");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        joinAs("guest", eventId);

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldRejectKickByNonHost() throws Exception {
        createUser("host");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        joinAs("guest", eventId);

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only host can delete Users"));
    }

    @Test
    void shouldReturnNotFoundWhenKickingUserNotAttending() throws Exception {
        createUser("host");
        Long strangerId = createUser("stranger").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, strangerId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isNotFound());
    }

    // ---------- update ----------

    @Test
    void shouldUpdateEvent() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "After Barcelona", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"After Updated\", \"capacity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After Updated"))
                .andExpect(jsonPath("$.capacity").value(5));
    }

    @Test
    void shouldUpdateOnlyFieldsSentInRequest() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Original", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"musicStyle\": \"HOUSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original"))
                .andExpect(jsonPath("$.eventType").value("AFTER"))
                .andExpect(jsonPath("$.musicStyle").value("HOUSE"));
    }

    @Test
    void shouldAllowCapacityEqualToAttendees() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        joinAs("guest", eventId);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(1))
                .andExpect(jsonPath("$.usersCount").value(1));
    }

    @Test
    void shouldReplaceAddressWhenSent() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"address": {"street": "Diagonal", "streetNum": "10", "postalCode": "08001",
                                             "city": "Barcelona", "province": "Barcelona"}}
                                """))
                .andExpect(status().isOk());

        assertEquals("Diagonal", eventRepository.findById(eventId).orElseThrow().getAddress().getStreet());
    }

    @Test
    void shouldRejectUpdateByNonHost() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("guest"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hacked\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only hosts can update Events"));
    }

    @Test
    void shouldRejectUpdateWithZeroCapacity() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\": 0}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- delete ----------

    @Test
    void shouldDeleteEvent() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(delete("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isNoContent());

        assertFalse(eventRepository.existsById(eventId));
    }

    @Test
    void shouldDeleteOnlyTheAddressOfTheDeletedEvent() throws Exception {
        createUser("host");
        Long deletedId = createEvent(tokenFor("host"), "First", 3);
        Long keptId = createEvent(tokenFor("host"), "Second", 3);
        Long deletedAddressId = eventRepository.findById(deletedId).orElseThrow().getAddress().getId();
        Long keptAddressId = eventRepository.findById(keptId).orElseThrow().getAddress().getId();

        mockMvc.perform(delete("/after/v1/events/{id}", deletedId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isNoContent());

        assertNull(entityManager.find(Address.class, deletedAddressId));
        assertNotNull(entityManager.find(Address.class, keptAddressId));
        assertTrue(eventRepository.existsById(keptId));
    }

    @Test
    void shouldRejectDeleteByNonHost() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(delete("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only host can delete own event"));
    }

    // ---------- an ADMIN who is not the host ----------

    @Test
    void shouldRejectAdminUpdatingEventOfAnotherHost() throws Exception {
        createUser("host");
        createUser("admin", "ADMIN");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Changed by admin\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdminInvitingToEventOfAnotherHost() throws Exception {
        createUser("host");
        createUser("admin", "ADMIN");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdminKickingFromEventOfAnotherHost() throws Exception {
        createUser("host");
        createUser("admin", "ADMIN");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        joinAs("guest", eventId);

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdminDeletingEventOfAnotherHost() throws Exception {
        createUser("host");
        createUser("admin", "ADMIN");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);

        mockMvc.perform(delete("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isForbidden());

        assertTrue(eventRepository.existsById(eventId));
    }

    // ---------- role changes with an existing token ----------

    @Test
    void shouldAllowInviteWithSameTokenAfterUpgradeToPremium() throws Exception {
        createUser("admin", "ADMIN");
        Long hostId = createUser("host", "FREE").getId();
        Long guestId = createUser("guest").getId();
        String hostToken = tokenFor("host");
        Long eventId = createEvent(hostToken, "Party", 3);

        changeRole(hostId, "PREMIUM");

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("Authorization", hostToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInviteWithSameTokenAfterDowngradeToFree() throws Exception {
        createUser("admin", "ADMIN");
        Long hostId = createUser("host", "PREMIUM").getId();
        Long guestId = createUser("guest").getId();
        String hostToken = tokenFor("host");
        Long eventId = createEvent(hostToken, "Party", 3);

        changeRole(hostId, "FREE");

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("Authorization", hostToken))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private void joinAs(String username, Long eventId) throws Exception {
        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor(username)))
                .andExpect(status().isOk());
    }

    private void changeRole(Long userId, String roleName) throws Exception {
        mockMvc.perform(patch("/after/v1/users/{id}/role", userId)
                        .header("Authorization", tokenFor("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\": \"" + roleName + "\"}"))
                .andExpect(status().isOk());
    }
}
