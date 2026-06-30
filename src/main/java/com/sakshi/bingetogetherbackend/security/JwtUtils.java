package com.sakshi.bingetogetherbackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private final String jwtSecret;
    private final int jwtExpirationMs = 86400000; // 24 Hours validity lifespan window
    private final Key signingKey;

    // Injecting the stable property string via the constructor safely
    public JwtUtils(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
        // Convert your application.properties string cleanly into a valid cryptokey
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Generates a token using the user's username identity
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(signingKey) // 🔥 Uses your permanent properties key now!
                .compact();
    }

    // Extracts the username text back out from an active token string
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey) // 🔥 Validates against the permanent key
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Cryptographically verify the token handle validity
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Invalid or Expired JWT Signature Token trace: " + e.getMessage());
        }
        return false;
    }
}