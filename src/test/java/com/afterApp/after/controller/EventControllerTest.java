package com.afterApp.after.controller;

import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private TokenUtil tokenUtil;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userAccessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateAndGetEventSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(get("/events/{id}", eventId)
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

        mockMvc.perform(get("/events")
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldFilterEventsByTypeAndStyle() throws Exception {
        String hostToken = createUserToken("hostuser");
        createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);
        createEvent(hostToken, "Pool Pop", "POOL_PARTY", "POP", 4);

        mockMvc.perform(get("/events")
                        .param("type", "AFTER")
                        .param("style", "TECHNO")
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("After Barcelona"));
    }

    @Test
    void shouldRejectGetEventsWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/events")
                        .header("authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Access denied"));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        String hostToken = createUserToken("hostuser");

        mockMvc.perform(get("/events/{id}", 999L)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectCreateEventWithInvalidBody() throws Exception {
        String hostToken = createUserToken("hostuser");

        mockMvc.perform(post("/events")
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

        mockMvc.perform(patch("/events/{id}/join", eventId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(1));

        mockMvc.perform(patch("/events/{id}/leave", eventId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldRejectJoinWhenEventIsFull() throws Exception {
        String hostToken = createUserToken("hostuser");
        String firstGuestToken = createUserToken("guestone");
        String secondGuestToken = createUserToken("guesttwo");
        Long eventId = createEvent(hostToken, "Small After", "AFTER", "TECHNO", 1);

        mockMvc.perform(patch("/events/{id}/join", eventId)
                        .header("authorization", bearer(firstGuestToken)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/events/{id}/join", eventId)
                        .header("authorization", bearer(secondGuestToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Event capacity is full"));
    }

    @Test
    void shouldInviteAndKickUserSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        createUser("guestuser");
        Long guestId = userId("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(1));

        mockMvc.perform(delete("/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    @Test
    void shouldRejectInviteByNonHost() throws Exception {
        String hostToken = createUserToken("hostuser");
        String guestToken = createUserToken("guestuser");
        createUser("targetuser");
        Long targetUserId = userId("targetuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/events/{eventId}/invite/user/{userId}", eventId, targetUserId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only host can invite Users"));
    }

    @Test
    void shouldRejectKickByNonHost() throws Exception {
        String hostToken = createUserToken("hostuser");
        String guestToken = createUserToken("guestuser");
        Long guestId = userId("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(patch("/events/{eventId}/invite/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/events/{eventId}/kick/user/{userId}", eventId, guestId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only host can delete Users"));
    }

    @Test
    void shouldDeleteEventSuccessfully() throws Exception {
        String hostToken = createUserToken("hostuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(delete("/events/{id}", eventId)
                        .header("authorization", bearer(hostToken)))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(eventRepository.existsById(eventId));
    }

    @Test
    void shouldRejectDeleteByNonHost() throws Exception {
        String hostToken = createUserToken("hostuser");
        String guestToken = createUserToken("guestuser");
        Long eventId = createEvent(hostToken, "After Barcelona", "AFTER", "TECHNO", 3);

        mockMvc.perform(delete("/events/{id}", eventId)
                        .header("authorization", bearer(guestToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only host can add Users"));
    }

    private Long createEvent(String token, String name, String type, String style, int capacity) throws Exception {
        mockMvc.perform(post("/events")
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

    private void createUser(String username) {
        Users user = new Users();
        user.setDisplayName(username);

        UserAccess access = new UserAccess();
        access.setUsername(username);
        access.setPassword("unused-in-controller-tests");
        access.setUser(user);

        userAccessRepository.save(access);
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
}
