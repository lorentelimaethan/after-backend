package com.afterApp.after.controller;

import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.service.UserAccessServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationErrorContractTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UserAccessServices accessService;

    @Test
    void shouldNotExposeDatabaseErrorWhenRegistrationConflicts() throws Exception {
        when(accessService.registerUser(any(RegisterDTO.class)))
                .thenThrow(new DataIntegrityViolationException("private SQL constraint details"));

        mvc.perform(post("/after/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"secret123","name":"New",
                                 "lastname":"User","email":"new@example.com","phoneNumber":"123456789"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(not(containsString("private SQL constraint details"))));
    }
}
