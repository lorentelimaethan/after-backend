package com.afterApp.after.controller;

import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.repositories.UserRoleRepository;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserAccessRepository userAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
        eventRepository.deleteAll();
        userAccessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateAndGetEventSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(get("/after/v1/events/{id}", eventId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.name").value("After Barcelona"))
                .andExpect(jsonPath("$.hostDisplayName").value("hostuser"))
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldGetAllEventsSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);
        createEvent(hostToken, "House Party", "HOUSE_PARTY", "HOUSE", 4);

        mockMvc.perform(get("/after/v1/events")
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldFilterEventsByTypeAndStyle() throws Exception {
        String hostToken = createUserToken("hostuser");
        createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);
        createEvent(hostToken, "Pool Pop", "POOL_PARTY", "POP", 4);

        mockMvc.perform(get("/after/v1/events")
                        .param("type", "AFTER")
                        .param("style", "TECHNO")
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("After Barcelona"));
    }

    @Test
    void shouldRejectGetEventsWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/after/v1/events")
                        .header("authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        String hostToken = createUserToken("hostuser");

        mockMvc.perform(get("/after/v1/events/{id}", 999L)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectCreateEventWithInvalidBody() throws Exception {
        String hostToken = createUserToken("hostuser");

        mockMvc.perform(post("/after/v1/events")
                        .header("authorization", bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Bad Event", "AFTER", "TECHNO", 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldJoinAndLeaveEventSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        String guestToken = createUserToken("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(1));

        mockMvc.perform(patch("/after/v1/events/{id}/leave", eventId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldRejectDuplicateJoinAndInvalidLeaves() throws Exception {
        String host = createUserToken("hostuser");
        String guest = createUserToken("guestuser");
        String stranger = createUserToken("stranger");
        Long id = createEvent(host, "Party", "AFTER", "TECHNO", 3);
        mockMvc.perform(patch("/after/v1/events/{id}/join", id).header("authorization", bearer(host)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/after/v1/events/{id}/leave", id).header("authorization", bearer(host)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/after/v1/events/{id}/leave", id).header("authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/after/v1/events/{id}/join", id).header("authorization", bearer(guest)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/after/v1/events/{id}/join", id).header("authorization", bearer(guest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvitingHostOrAttendeeAndKickingNonAttendee() throws Exception {
        String host = createUserToken("hostuser", "ADMIN");
        createUser("guestuser");
        createUser("stranger");
        Long id = createEvent(host, "Party", "AFTER", "TECHNO", 3);
        Long hostId = userId("hostuser");
        Long guestId = userId("guestuser");
        Long strangerId = userId("stranger");
        mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{userId}", id, hostId)
                        .header("authorization", bearer(host))).andExpect(status().isBadRequest());
        mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{userId}", id, guestId)
                        .header("authorization", bearer(host))).andExpect(status().isOk());
        mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{userId}", id, guestId)
                        .header("authorization", bearer(host))).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/after/v1/events/{id}/kick/user/{userId}", id, strangerId)
                        .header("authorization", bearer(host))).andExpect(status().isNotFound());
    }

    @Test
    void shouldApplyRoleChangesToExistingToken() throws Exception {
        String admin = createUserToken("adminuser", "ADMIN");
        String host = createUserToken("hostuser", "FREE");
        createUser("guestuser");
        Long hostId = userId("hostuser");
        Long guestId = userId("guestuser");
        Long id = createEvent(host, "Party", "AFTER", "TECHNO", 3);
        mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{guest}", id, guestId)
                        .header("authorization", bearer(host))).andExpect(status().isForbidden());
        mockMvc.perform(patch("/after/v1/users/{id}/role", hostId).header("authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleName\":\"PREMIUM\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{guest}", id, guestId)
                        .header("authorization", bearer(host))).andExpect(status().isOk());
        mockMvc.perform(patch("/after/v1/users/{id}/role", hostId).header("authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleName\":\"FREE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{guest}", id, guestId)
                        .header("authorization", bearer(host))).andExpect(status().isForbidden());
    }

    @Test
    void shouldInvalidateCachedEventAfterUpdate() throws Exception {
        String host = createUserToken("hostuser");
        Long id = createEvent(host, "Original", "AFTER", "TECHNO", 3);
        mockMvc.perform(get("/after/v1/events/{id}", id).header("authorization", bearer(host)))
                .andExpect(jsonPath("$.name").value("Original"));
        mockMvc.perform(patch("/after/v1/events/{id}", id).header("authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/after/v1/events/{id}", id).header("authorization", bearer(host)))
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void shouldRefreshCachedEventAndFilteredResultsAfterAttendanceAndDelete() throws Exception {
        String host = createUserToken("hostuser", "ADMIN");
        String guest = createUserToken("guestuser");
        createUser("invited");
        Long invited = userId("invited");
        Long id = createEvent(host, "Party", "AFTER", "TECHNO", 3);
        assertCachedCounts(host, id, 0);
        mockMvc.perform(patch("/after/v1/events/{id}/join", id).header("authorization", bearer(guest)))
                .andExpect(status().isOk());
        assertCachedCounts(host, id, 1);
        mockMvc.perform(patch("/after/v1/events/{id}/leave", id).header("authorization", bearer(guest)))
                .andExpect(status().isOk());
        assertCachedCounts(host, id, 0);
        mockMvc.perform(patch("/after/v1/events/{id}/invite/user/{user}", id, invited)
                        .header("authorization", bearer(host))).andExpect(status().isOk());
        assertCachedCounts(host, id, 1);
        mockMvc.perform(delete("/after/v1/events/{id}/kick/user/{user}", id, invited)
                        .header("authorization", bearer(host))).andExpect(status().isOk());
        assertCachedCounts(host, id, 0);
        mockMvc.perform(delete("/after/v1/events/{id}", id).header("authorization", bearer(host)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/after/v1/events/{id}", id).header("authorization", bearer(host)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/after/v1/events").param("type", "AFTER").header("authorization", bearer(host)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void assertCachedCounts(String token, Long id, int expected) throws Exception {
        mockMvc.perform(get("/after/v1/events/{id}", id).header("authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usersCount").value(expected));
        mockMvc.perform(get("/after/v1/events").param("type", "AFTER").header("authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].usersCount").value(expected));
    }

    @Test
    void shouldRefreshCachedFilteredListAfterCreatingEvent() throws Exception {
        String host = createUserToken("hostuser");
        mockMvc.perform(get("/after/v1/events").param("type", "AFTER").header("authorization", bearer(host)))
                .andExpect(jsonPath("$.length()").value(0));
        createEvent(host, "New event", "AFTER", "TECHNO", 3);
        mockMvc.perform(get("/after/v1/events").param("type", "AFTER").header("authorization", bearer(host)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldRejectJoinWhenEventIsFull() throws Exception {
        String hostToken = createUserToken("hostuser");
        String firstGuestToken = createUserToken("guestone");
        String secondGuestToken = createUserToken("guesttwo");
        Long eventId = createEvent(hostToken, "Small After", "AFTER", "TECHNO", 1);

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("authorization", bearer(firstGuestToken)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId)
                        .header("authorization", bearer(secondGuestToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Event capacity is full"));
    }

    @Test
    void shouldInviteAndKickUserSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser", "ADMIN");
        createUser("guestuser");
        Long guestId = userId("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(1));

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldRejectInviteByNonHost() throws Exception {
        String hostToken = createUserToken("hostuser");
        String guestToken = createUserToken("guestuser", "PREMIUM");
        createUser("targetuser");
        Long targetUserId = userId("targetuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, targetUserId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only host can invite Users"));
    }

    @Test
    void shouldRejectKickByNonHost() throws Exception {
        String hostToken = createUserToken("hostuser", "ADMIN");
        String guestToken = createUserToken("guestuser", "ADMIN");
        Long guestId = userId("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only host can delete Users"));
    }

    @Test
    void shouldRejectInviteWithoutInviteUserPermission() throws Exception {
        String hostToken = createUserToken("hostuser");
        String freeToken = createUserToken("freeuser");
        createUser("targetuser");
        Long targetUserId = userId("targetuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, targetUserId)
                        .header("authorization", bearer(freeToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectKickWithoutKickUserPermission() throws Exception {
        String hostToken = createUserToken("hostuser", "ADMIN");
        String premiumToken = createUserToken("premiumuser", "PREMIUM");
        createUser("guestuser");
        Long guestId = userId("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/after/v1/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(premiumToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateEventSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("authorization", bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateEventJson("After Updated", 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After Updated"))
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.hostDisplayName").value("hostuser"));
    }

    @Test
    void shouldUpdateOnlySpecifiedFieldsAndAllowCapacityEqualToAttendees() throws Exception {
        String host = createUserToken("hostuser");
        String guest = createUserToken("guestuser");
        Long id = createEvent(host, "Original", "AFTER", "TECHNO", 3);
        mockMvc.perform(patch("/after/v1/events/{id}/join", id).header("authorization", bearer(guest)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/after/v1/events/{id}", id).header("authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"capacity":1,"musicStyle":"HOUSE",
                                 "address":{"street":"Diagonal","streetNum":"10","postalCode":"08001",
                                            "city":"Barcelona","province":"Barcelona"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original"))
                .andExpect(jsonPath("$.eventType").value("AFTER"))
                .andExpect(jsonPath("$.musicStyle").value("HOUSE"))
                .andExpect(jsonPath("$.capacity").value(1))
                .andExpect(jsonPath("$.usersCount").value(1));
        org.junit.jupiter.api.Assertions.assertEquals("Diagonal", eventRepository.findById(id).orElseThrow().getAddress().getStreet());
    }

    @Test
    void shouldRejectUpdateEventByNonHost() throws Exception {
        String hostToken = createUserToken("hostuser");
        String guestToken = createUserToken("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("authorization", bearer(guestToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateEventJson("After Updated", 5)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only hosts can update Events"));
    }

    @Test
    void shouldRejectUpdateEventWithInvalidBody() throws Exception {
        String hostToken = createUserToken("hostuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/after/v1/events/{id}", eventId)
                        .header("authorization", bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateEventJson("After Updated", 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteEventSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(delete("/after/v1/events/{id}", eventId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(eventRepository.existsById(eventId));
    }

    @Test
    void shouldRejectDeleteByNonHost() throws Exception {
        String hostToken = createUserToken("hostuser");
        String guestToken = createUserToken("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(delete("/after/v1/events/{id}", eventId)
                .header("authorization", bearer(guestToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only host can delete own event"));
    }

    private Long createEvent(String token, String name, String type, String style, int capacity) throws Exception {
        mockMvc.perform(post("/after/v1/events")
                        .header("authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(name, type, style, capacity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        return eventRepository.findAll().stream()
                .filter(event -> name.equals(event.getName()))
                .map(Events::getId)
                .findFirst()
                .orElseThrow();
    }

    private String createUserToken(String username) {
        createUser(username);
        return tokenUtil.generateToken(username);
    }

    private String createUserToken(String username, String roleName) {
        createUser(username, roleName);
        return tokenUtil.generateToken(username);
    }

    private void createUser(String username) {
        createUser(username, "FREE");
    }

    private void createUser(String username, String roleName) {
        Users user = new Users();
        user.setDisplayName(username);
        user.setUserRole(role(roleName));

        UserAccess access = new UserAccess();
        access.setUsername(username);
        access.setPassword("unused-in-controller-tests");
        access.setUser(user);

        userAccessRepository.save(access);
    }

    private UserRole role(String roleName) {
        return userRoleRepository.findByRoleName(roleName).orElseThrow();
    }

    private Long userId(String username) {
        return userAccessRepository.findByUsername(username).orElseThrow().getUser().getId();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String eventJson(String name, String type, String style, int capacity) {
        return """
                {
                  "name": "%s",
                  "eventType": "%s",
                  "musicStyle": "%s",
                  "description": "HTTP integration test event",
                  "dateTime": "2030-06-05T23:00:00",
                  "capacity": %d,
                  "address": {
                    "street": "Carrer Marina",
                    "streetNum": "25",
                    "postalCode": "08005",
                    "city": "Barcelona",
                    "province": "Barcelona",
                    "additionalInfo": "Test address"
                  }
                }
                """.formatted(name, type, style, capacity);
    }

    private String updateEventJson(String name, int capacity) {
        return """
                {
                  "name": "%s",
                  "capacity": %d
                }
                """.formatted(name, capacity);
    }
}
