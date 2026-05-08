package io.ioteca.gateway.service;

import io.ioteca.gateway.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-32-chars-minimum!";
    private JwtService jwtService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtConfig(SECRET, 60L, 30L));
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ── claim extraction ──────────────────────────────────────────────────────

    @Test
    void extractEmail_returnsEmailClaim() {
        String token = buildToken("email", "user@ioteca.io", "role", "USER", "type", "access");
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@ioteca.io");
    }

    @Test
    void extractRole_returnsRoleClaim() {
        String token = buildToken("email", "user@ioteca.io", "role", "ADMIN", "type", "access");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractUserId_returnsSubjectAsUUID() {
        UUID userId = UUID.randomUUID();
        String token = buildTokenForSubject(userId.toString(), "type", "access");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    // ── token type ────────────────────────────────────────────────────────────

    @Test
    void isRefreshToken_falseForAccessToken() {
        String token = buildToken("type", "access");
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void isRefreshToken_trueForRefreshToken() {
        String token = buildToken("type", "refresh");
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    // ── validity ──────────────────────────────────────────────────────────────

    @Test
    void isTokenValid_trueForFreshToken() {
        String token = buildTokenWithExpiry(new Date(System.currentTimeMillis() + 60_000));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_falseForExpiredToken() {
        String token = buildTokenWithExpiry(new Date(System.currentTimeMillis() - 1_000));
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_falseForGarbageInput() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildToken(Object... claimPairs) {
        return buildTokenForSubject(UUID.randomUUID().toString(), claimPairs);
    }

    private String buildTokenForSubject(String subject, Object... claimPairs) {
        var builder = Jwts.builder().subject(subject)
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        for (int i = 0; i < claimPairs.length; i += 2) {
            builder.claim((String) claimPairs[i], claimPairs[i + 1]);
        }
        return builder.signWith(signingKey).compact();
    }

    private String buildTokenWithExpiry(Date expiry) {
        return Jwts.builder().subject(UUID.randomUUID().toString())
                .claim("type", "access").expiration(expiry)
                .signWith(signingKey).compact();
    }
}
