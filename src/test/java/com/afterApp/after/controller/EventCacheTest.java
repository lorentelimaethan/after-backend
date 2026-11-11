package com.afterApp.after.controller;

import com.afterApp.after.support.HttpTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Events are cached. Each test reads an event first (so it is stored in the cache),
 * changes it, and checks that the next read returns the new data.
 */
class EventCacheTest extends HttpTestBase {

    @Test
    void shouldShowNewNameAfterUpdate() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Original", 3);
        readEvent(eventId);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Updated\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void shouldShowNewAttendeeAfterJoin() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        readEvent(eventId);
        readFilteredList();

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isOk());

        assertEquals(1, usersCount(eventId, tokenFor("host")));
        assertEquals(1, usersCountInFilteredList());
    }

    @Test
    void shouldShowAttendeeRemovedAfterLeave() throws Exception {
        createUser("host");
        createUser("guest");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isOk());
        readEvent(eventId);
        readFilteredList();

        mockMvc.perform(patch("/after/v1/events/{id}/leave", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isOk());

        assertEquals(0, usersCount(eventId, tokenFor("host")));
        assertEquals(0, usersCountInFilteredList());
    }

    @Test
    void shouldShowInvitedUserAfterInvite() throws Exception {
        createUser("host", "PREMIUM");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        readEvent(eventId);
        readFilteredList();

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk());

        assertEquals(1, usersCount(eventId, tokenFor("host")));
        assertEquals(1, usersCountInFilteredList());
    }

    @Test
    void shouldShowKickedUserRemovedAfterKick() throws Exception {
        createUser("host");
        Long guestId = createUser("guest").getId();
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("Authorization", tokenFor("guest")))
                .andExpect(status().isOk());
        readEvent(eventId);
        readFilteredList();

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk());

        assertEquals(0, usersCount(eventId, tokenFor("host")));
        assertEquals(0, usersCountInFilteredList());
    }

    @Test
    void shouldNotReturnEventAfterDelete() throws Exception {
        createUser("host");
        Long eventId = createEvent(tokenFor("host"), "Party", 3);
        readEvent(eventId);
        readFilteredList();

        mockMvc.perform(delete("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/after/v1/events").param("type", "AFTER")
                        .header("Authorization", tokenFor("host")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldShowNewEventInFilteredListAfterCreate() throws Exception {
        createUser("host");
        readFilteredList();

        createEvent(tokenFor("host"), "New event", 3);

        mockMvc.perform(get("/after/v1/events").param("type", "AFTER")
                        .header("Authorization", tokenFor("host")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- helpers ----------

    private void readEvent(Long eventId) throws Exception {
        mockMvc.perform(get("/after/v1/events/{id}", eventId)
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk());
    }

    private void readFilteredList() throws Exception {
        mockMvc.perform(get("/after/v1/events").param("type", "AFTER")
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk());
    }

    private int usersCountInFilteredList() throws Exception {
        String response = mockMvc.perform(get("/after/v1/events").param("type", "AFTER")
                        .header("Authorization", tokenFor("host")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$[0].usersCount");
    }
}
