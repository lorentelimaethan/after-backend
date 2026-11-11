package com.afterApp.after.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TokenUtilTest {

    private static final String OTHER_SECRET = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU=";

    @Autowired
    private TokenUtil tokenUtil;

    // The same secret the application uses (environment variable or application-test.properties)
    @Value("${jwt.secret}")
    private String secret;

    @Test
    void shouldValidateGeneratedToken() {
        String token = tokenUtil.generateToken("ethan");

        assertTrue(tokenUtil.validateToken("Bearer " + token));
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = tokenUtil.generateToken("ethan");

        assertEquals("ethan", tokenUtil.extractUsername("Bearer " + token));
    }

    @Test
    void shouldRejectTokenAfterLogout() {
        String token = tokenUtil.generateToken("logout-user");

        tokenUtil.invalidateToken("Bearer " + token);

        assertFalse(tokenUtil.validateToken("Bearer " + token));
    }

    @Test
    void shouldRejectExpiredToken() {
        String expired = Jwts.builder()
                .subject("ethan")
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(key(secret))
                .compact();

        assertFalse(tokenUtil.validateToken("Bearer " + expired));
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() {
        String forged = Jwts.builder()
                .subject("ethan")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key(OTHER_SECRET))
                .compact();

        assertFalse(tokenUtil.validateToken("Bearer " + forged));
    }

    @Test
    void shouldRejectMalformedToken() {
        assertFalse(tokenUtil.validateToken("Bearer malformed"));
    }

    private SecretKey key(String base64Secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }
}
