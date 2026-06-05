package com.afterApp.after.controller;

import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @BeforeEach
    void setUp() {
        userAccessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        mockMvc.perform(post("/token/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ethanlo", "secret123")))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario creado correctamente"));

        var access = userAccessRepository.findByUsername("ethanlo").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ethanlo", access.getUser().getDisplayName());
    }

    @Test
    void shouldRejectRegisterWithInvalidBody() throws Exception {
        mockMvc.perform(post("/token/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("", "123")))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, userAccessRepository.count());
    }

    @Test
    void shouldRejectRegisterWhenUsernameAlreadyExists() throws Exception {
        register("ethanlo", "secret123");

        mockMvc.perform(post("/token/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ethanlo", "secret123")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username already exists"));
    }

    @Test
    void shouldLoginSuccessfullyAndReturnToken() throws Exception {
        register("ethanlo", "secret123");

        mockMvc.perform(post("/token/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ethanlo", "secret123")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(blankOrNullString())));
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        register("ethanlo", "secret123");

        mockMvc.perform(post("/token/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ethanlo", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Access denied"));
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/token/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, password)))
                .andExpect(status().isOk());
    }

    private String registerJson(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }

    private String loginJson(String username, String password) {
        return registerJson(username, password);
    }
}
