package com.afterApp.after.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilTest {
    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private TokenUtil tokens;

    @BeforeEach
    void setUp() {
        tokens = new TokenUtil();
        ReflectionTestUtils.setField(tokens, "secret", SECRET);
        tokens.init();
    }

    @Test
    void shouldValidateExtractAndInvalidateToken() {
        String token = tokens.generateToken("ethan");
        assertTrue(tokens.validateToken("Bearer " + token));
        assertEquals("ethan", tokens.extractUsername("Bearer " + token));
        tokens.invalidateToken("Bearer " + token);
        assertFalse(tokens.validateToken("Bearer " + token));
    }

    @Test
    void shouldRejectExpiredAndIncorrectlySignedTokens() {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String expired = Jwts.builder().subject("ethan").expiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(key).compact();
        var otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU="));
        String forged = Jwts.builder().subject("ethan").expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey).compact();
        assertFalse(tokens.validateToken("Bearer " + expired));
        assertFalse(tokens.validateToken("Bearer " + forged));
        assertFalse(tokens.validateToken("Bearer malformed"));
    }
}
