package com.afterApp.after.security;

import com.afterApp.after.support.HttpTestBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every request below uses a wrong Authorization header and must get 401. */
class AuthenticationHttpTest extends HttpTestBase {

    private static final String OTHER_SECRET = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU=";

    // The same secret the application uses (environment variable or application-test.properties)
    @Value("${jwt.secret}")
    private String secret;

    private Long userId;

    @BeforeEach
    void createTokenOwner() {
        userId = createUser("token-owner").getId();
    }

    @Test
    void shouldRejectRequestWithoutHeader() throws Exception {
        mockMvc.perform(get("/after/v1/users/{id}", userId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMalformedToken() throws Exception {
        expectUnauthorized("Bearer malformed");
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        String expired = Jwts.builder()
                .subject("token-owner")
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();

        expectUnauthorized("Bearer " + expired);
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() throws Exception {
        String forged = Jwts.builder()
                .subject("token-owner")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(OTHER_SECRET)))
                .compact();

        expectUnauthorized("Bearer " + forged);
    }

    @Test
    void shouldRejectTokenWithoutBearerPrefix() throws Exception {
        expectUnauthorized(tokenUtil.generateToken("token-owner"));
    }

    @Test
    void shouldRejectBasicAuthentication() throws Exception {
        expectUnauthorized("Basic credentials");
    }

    @Test
    void shouldRejectLogoutWithoutToken() throws Exception {
        mockMvc.perform(post("/after/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLogoutWithMalformedToken() throws Exception {
        mockMvc.perform(post("/after/v1/auth/logout").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
    }

    private void expectUnauthorized(String authorizationHeader) throws Exception {
        mockMvc.perform(get("/after/v1/users/{id}", userId)
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
