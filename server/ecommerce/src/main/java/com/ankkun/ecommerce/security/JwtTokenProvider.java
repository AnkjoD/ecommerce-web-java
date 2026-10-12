package com.ankkun.ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@lombok.RequiredArgsConstructor
public class JwtTokenProvider {
    private final com.ankkun.ecommerce.module.user.repository.UserRepository users;

    @Value("${jwt.access-secret}")
    private String accessSecret;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, String email, String role) {
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim("ver",users.findById(userId).orElseThrow().getTokenVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(accessKey())
                .compact();
    }

    public String generateRefreshToken(String userId, String email, String role) {
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim("ver",users.findById(userId).orElseThrow().getTokenVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(refreshKey())
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(accessKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser().verifyWith(refreshKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
