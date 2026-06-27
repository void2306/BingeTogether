package com.sakshi.bingetogetherbackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    // A secure 256-bit signing key generated safely for HMAC-SHA algorithms
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token validity lifespan window: 24 Hours (in milliseconds)
    private final int jwtExpirationMs = 86400000;

    // Generates a token using the user's username handle identity
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    // Extracts the username text back out from an active token string
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // Checks if a token has been tampered with or has expired past its window limit
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Invalid or Expired JWT Signature Token trace: " + e.getMessage());
        }
        return false;
    }
}