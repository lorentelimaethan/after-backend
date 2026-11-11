package com.afterApp.after.controller;

import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.service.UserAccessServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The registration service is replaced by a mock to force each error. */
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationErrorContractTest {

    private static final String REGISTER_JSON = """
            {"username": "newuser", "password": "secret123", "name": "New",
             "lastname": "User", "email": "new@example.com", "phoneNumber": "123456789"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccessServices userAccessServices;

    @Test
    void shouldReturn409WhenUsernameExists() throws Exception {
        when(userAccessServices.registerUser(any(RegisterDTO.class)))
                .thenThrow(new AlreadyExistsException("Username already exists"));

        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void shouldReturn500WithoutDatabaseDetails() throws Exception {
        when(userAccessServices.registerUser(any(RegisterDTO.class)))
                .thenThrow(new DataIntegrityViolationException("private SQL constraint details"));

        mockMvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected Server Error"))
                .andExpect(content().string(not(containsString("private SQL constraint details"))));
    }
}
