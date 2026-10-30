package com.example.scheduler.security;

import com.example.scheduler.entity.Clinic;
import com.example.scheduler.enums.ERole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {
    public static final String GUEST_SUBJECT = "0";
    public static final Duration GUEST_TTL = Duration.ofMinutes(30);

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String role, Clinic clinic, String userName) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("clinicId", clinic.getId())
                .claim("clinicName", clinic.getName())
                .claim("clinicSlug", clinic.getSlug())
                .claim("username", userName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(key)
                .compact();
    }

    public String generateGuest(Clinic clinic, Instant expiresAt) {
        return Jwts.builder()
                .subject(GUEST_SUBJECT)
                .claim("role", ERole.GUEST.name())
                .claim("clinicId", clinic.getId())
                .claim("clinicName", clinic.getName())
                .claim("clinicSlug", clinic.getSlug())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}
