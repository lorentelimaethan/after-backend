package com.afterApp.after.controller;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.support.HttpTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAccessControllerTest extends HttpTestBase {

    // ---------- register ----------

    @Test
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ethan", "secret123")))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario creado correctamente"));

        UserAccess access = userAccessRepository.findByUsername("ethan").orElseThrow();
        assertEquals("ethan", access.getUser().getDisplayName());
        assertEquals("FREE", access.getUser().getUserRole().getRoleName());
    }

    @Test
    void shouldSaveHashedPasswordWhenRegistering() throws Exception {
        register("ethan", "secret123");

        UserAccess access = userAccessRepository.findByUsername("ethan").orElseThrow();
        assertTrue(new BCryptPasswordEncoder().matches("secret123", access.getPassword()));
    }

    @Test
    void shouldRejectRegisterWithEmptyUsernameAndShortPassword() throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("", "123")))
                .andExpect(status().isBadRequest());

        assertEquals(0, userAccessRepository.count());
    }

    @Test
    void shouldReturnAllMissingRegisterFields() throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ethan\", \"password\": \"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.lastname").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.phoneNumber").exists());
    }

    @Test
    void shouldReturnConflictWhenUsernameExists() throws Exception {
        register("ethan", "secret123");

        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ethan", "secret123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    // ---------- login ----------

    @Test
    void shouldLoginAndReturnValidToken() throws Exception {
        register("ethan", "secret123");

        String token = login("ethan", "secret123");

        assertTrue(tokenUtil.validateToken("Bearer " + token));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        register("ethan", "secret123");

        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ethan", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldRejectLoginWithUnknownUser() throws Exception {
        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown", "secret123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWithoutPassword() throws Exception {
        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ethan\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password required"));
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest());
    }

    // ---------- logout ----------

    @Test
    void shouldRejectTokenAfterLogout() throws Exception {
        register("ethan", "secret123");
        String token = "Bearer " + login("ethan", "secret123");
        Long userId = userAccessRepository.findByUsername("ethan").orElseThrow().getUser().getId();

        mockMvc.perform(post("/after/v1/auth/logout").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/after/v1/users/{id}", userId).header("Authorization", token))
                .andExpect(status().isUnauthorized());
        assertFalse(tokenUtil.validateToken(token));
    }

    // ---------- complete flow ----------

    /** Main flow of the app: register → login → create an event → another user joins and leaves. */
    @Test
    void shouldRegisterLoginCreateEventJoinAndLeave() throws Exception {
        register("host", "secret123");
        register("guest", "secret123");
        String hostToken = "Bearer " + login("host", "secret123");
        String guestToken = "Bearer " + login("guest", "secret123");
        Long eventId = createEvent(hostToken, "Flow Party", 3);

        mockMvc.perform(patch("/after/v1/events/{id}/join", eventId).header("Authorization", guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(1));

        mockMvc.perform(patch("/after/v1/events/{id}/leave", eventId).header("Authorization", guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(0));
    }

    // ---------- helpers ----------

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, password)))
                .andExpect(status().isOk());
    }

    /** Returns the token (without "Bearer "). */
    private String login(String username, String password) throws Exception {
        return mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String registerJson(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s",
                  "name": "Ethan",
                  "lastname": "Lorente",
                  "email": "%s@example.com",
                  "phoneNumber": "+34612345678"
                }
                """.formatted(username, password, username);
    }

    private String loginJson(String username, String password) {
        return "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
    }
}
