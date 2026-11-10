package com.afterApp.after.security;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import com.afterApp.after.utils.TokenUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private TokenUtil tokens;
    @Autowired private UserAccessRepository accesses;
    @Autowired private UserRepository users;
    @Autowired private UserRoleRepository roles;
    @Autowired private EventRepository events;

    @BeforeEach
    void setUp() {
        events.deleteAll();
        accesses.deleteAll();
        users.deleteAll();
    }

    @Test
    void shouldRejectMissingMalformedExpiredAndForgedAuthentication() throws Exception {
        Users user = new Users();
        user.setDisplayName("token-owner");
        user.setUserRole(roles.findByRoleName("FREE").orElseThrow());
        UserAccess access = new UserAccess();
        access.setUsername("token-owner");
        access.setPassword("unused");
        access.setUser(user);
        accesses.save(access);
        Long id = access.getUser().getId();

        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="));
        var otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU="));
        String expired = Jwts.builder().subject("token-owner")
                .expiration(new Date(System.currentTimeMillis() - 10_000)).signWith(key).compact();
        String forged = Jwts.builder().subject("token-owner")
                .expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(otherKey).compact();

        mvc.perform(get("/after/v1/users/{id}", id))
                .andExpect(status().isUnauthorized());
        for (String header : new String[] {"Bearer malformed", "Bearer " + expired, "Bearer " + forged,
                tokens.generateToken("token-owner"), "Basic credentials"}) {
            mvc.perform(get("/after/v1/users/{id}", id).header("Authorization", header))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }
    }

    @Test
    void shouldRejectLogoutWithoutAuthentication() throws Exception {
        mvc.perform(post("/after/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/after/v1/auth/logout").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
    }
}
