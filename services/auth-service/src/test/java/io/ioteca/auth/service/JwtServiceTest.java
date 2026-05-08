package io.ioteca.auth.service;

import io.ioteca.auth.config.JwtConfig;
import io.ioteca.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        // 32-char secret satisfies HMAC-SHA minimum key length
        JwtConfig config = new JwtConfig("test-secret-key-32-chars-minimum!", 60L, 30L);
        jwtService = new JwtService(config);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@ioteca.io")
                .role("USER")
                .build();
    }

    @Test
    void generateAccessToken_returnsNonBlankToken() {
        String token = jwtService.generateAccessToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_returnsNonBlankToken() {
        String token = jwtService.generateRefreshToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void accessToken_claimsMatchUser() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractRole(token)).isEqualTo(user.getRole());
    }

    @Test
    void isRefreshToken_falseForAccessToken() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void isRefreshToken_trueForRefreshToken() {
        String token = jwtService.generateRefreshToken(user);
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test
    void isTokenValid_trueForFreshToken() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_falseForGarbage() {
        assertThat(jwtService.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    void isTokenValid_falseForExpiredToken() {
        // 0-minute expiry → immediately expired
        JwtConfig shortConfig = new JwtConfig("test-secret-key-32-chars-minimum!", 0L, 30L);
        JwtService shortService = new JwtService(shortConfig);
        String token = shortService.generateAccessToken(user);
        assertThat(shortService.isTokenValid(token)).isFalse();
    }

    @Test
    void accessTokenExpirySeconds_matchesConfig() {
        assertThat(jwtService.accessTokenExpirySeconds()).isEqualTo(60 * 60L);
    }

    @Test
    void adminUserHasAdminRole() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("admin@ioteca.io")
                .role("ADMIN")
                .build();
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }
}
