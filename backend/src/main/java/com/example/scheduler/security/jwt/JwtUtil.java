package com.example.scheduler.security.jwt;

import com.example.scheduler.entity.Clinic;
import com.example.scheduler.enums.ERole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    public static final String GUEST_SUBJECT = "0";
    public static final Duration GUEST_TTL = Duration.ofMinutes(30);
    public static final Duration STAFF_TTL = Duration.ofHours(24);

    private final SecretKey key;
    private final Clock clock;

    public JwtUtil(@Value("${jwt.secret}") String secret, Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String generate(Long userId, String role, Clinic clinic, String userName) {
        var now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(JwtClaims.ROLE, role)
                .claim(JwtClaims.CLINIC_ID, clinic.getId())
                .claim(JwtClaims.CLINIC_NAME, clinic.getName())
                .claim(JwtClaims.CLINIC_SLUG, clinic.getSlug())
                .claim(JwtClaims.USERNAME, userName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(STAFF_TTL)))
                .signWith(key)
                .compact();
    }

    public String generateGuest(Clinic clinic, Instant expiresAt) {
        return Jwts.builder()
                .subject(GUEST_SUBJECT)
                .claim(JwtClaims.ROLE, ERole.GUEST.name())
                .claim(JwtClaims.CLINIC_ID, clinic.getId())
                .claim(JwtClaims.CLINIC_NAME, clinic.getName())
                .claim(JwtClaims.CLINIC_SLUG, clinic.getSlug())
                .issuedAt(Date.from(clock.instant()))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}
