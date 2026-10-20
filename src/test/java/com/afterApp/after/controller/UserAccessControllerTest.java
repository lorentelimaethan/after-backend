package com.afterApp.after.controller;

import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccessRepository userAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userAccessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ethanlo", "secret123")))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario creado correctamente"));

        var access = userAccessRepository.findByUsername("ethanlo").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ethanlo", access.getUser().getDisplayName());
        org.junit.jupiter.api.Assertions.assertEquals("FREE", access.getUser().getUserRole().getRoleName());
        org.junit.jupiter.api.Assertions.assertNotEquals("secret123", access.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(new BCryptPasswordEncoder().matches("secret123", access.getPassword()));
    }

    @Test
    void shouldRejectRegisterWithInvalidBody() throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("", "123")))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, userAccessRepository.count());
    }

    @Test
    void shouldRejectRegisterWhenUsernameAlreadyExists() throws Exception {
        register("ethanlo", "secret123");

        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ethanlo", "secret123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void shouldLoginSuccessfullyAndReturnToken() throws Exception {
        register("ethanlo", "secret123");

        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ethanlo", "secret123")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(blankOrNullString())));
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        register("ethanlo", "secret123");

        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ethanlo", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldRejectLoginWithoutPassword() throws Exception {
        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ethanlo"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password required"));
    }

    @Test
    void shouldRejectUnknownUserAndMalformedJson() throws Exception {
        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown", "secret123")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingRegistrationFields() throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ethanlo","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.lastname").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.phoneNumber").exists());
    }

    @Test
    void shouldInvalidateTokenAfterLogout() throws Exception {
        register("logoutuser", "secret123");
        String token = mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("logoutuser", "secret123")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String authorization = "Bearer " + token;
        Long id = userAccessRepository.findByUsername("logoutuser").orElseThrow().getUser().getId();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/after/v1/users/{id}", id)
                        .header("authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(post("/after/v1/auth/logout").header("authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/after/v1/users/{id}", id)
                        .header("authorization", authorization))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCompleteRegistrationLoginAndEventAttendanceFlow() throws Exception {
        register("flowhost", "secret123");
        register("flowguest", "secret123");
        String host = login("flowhost", "secret123");
        String guest = login("flowguest", "secret123");
        mockMvc.perform(post("/after/v1/events")
                        .header("authorization", "Bearer " + host)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Flow Party","description":"Flow test","eventType":"AFTER",
                                 "musicStyle":"TECHNO","dateTime":"2030-01-01T22:00:00","capacity":3,
                                 "address":{"street":"Marina","streetNum":"25","postalCode":"08005",
                                            "city":"Barcelona","province":"Barcelona"}}
                                """))
                .andExpect(status().isOk());
        Long id = eventRepository.findAll().stream().filter(event -> event.getName().equals("Flow Party"))
                .findFirst().orElseThrow().getId();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/after/v1/events/{id}/join", id)
                        .header("authorization", "Bearer " + guest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usersCount").value(1));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/after/v1/events/{id}/leave", id)
                        .header("authorization", "Bearer " + guest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usersCount").value(0));
    }

    private String login(String username, String password) throws Exception {
        return mockMvc.perform(post("/after/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginJson(username, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, password)))
                .andExpect(status().isOk());
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
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }
}
