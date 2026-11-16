package com.afterApp.after.utils;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class TokenUtil {
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    private final Set<String> blockedToken = new HashSet<>();

    @PostConstruct
    public void init(){
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(String username){
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key)
                .compact();
    }

    public Boolean validateToken(String authorization){
        try {
            String token = extractToken(authorization);

            if(blockedToken.contains(token)){
                return false;
            }

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        }catch (JwtException | IllegalArgumentException exception){
            return false;
        }
    }

    public String extractUsername(String authorization){
        String token = extractToken(authorization);

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private String extractToken(String authorization){
        return authorization.replace("Bearer ", "");
    }

    public void invalidateToken(String authorization){
        String token = extractToken(authorization);

        blockedToken.add(token);
    }
}
