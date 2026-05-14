package io.ioteca.auth;

import io.ioteca.auth.dto.AuthResponse;
import io.ioteca.auth.dto.LoginRequest;
import io.ioteca.auth.dto.RegisterRequest;
import io.ioteca.auth.dto.TokenRefreshRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class AuthServiceIT {

    @Autowired
    TestRestTemplate rest;

    static String accessToken;
    static String refreshToken;
    static final String EMAIL = "it-user@ioteca.io";
    static final String PASSWORD = "SecurePass123!";

    // ── register ──────────────────────────────────────────────────────────────

    @Test @Order(1)
    void register_returnsCreatedWithTokens() {
        var req = new RegisterRequest(EMAIL, PASSWORD, "IT User", null, "KE");

        ResponseEntity<AuthResponse> resp =
                rest.postForEntity("/api/v1/auth/register", req, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().accessToken()).isNotBlank();
        assertThat(resp.getBody().refreshToken()).isNotBlank();
        assertThat(resp.getBody().email()).isEqualTo(EMAIL);

        accessToken  = resp.getBody().accessToken();
        refreshToken = resp.getBody().refreshToken();
    }

    @Test @Order(2)
    void register_duplicateEmail_returns400() {
        var req = new RegisterRequest(EMAIL, PASSWORD, "Duplicate", null, "KE");

        ResponseEntity<String> resp =
                rest.postForEntity("/api/v1/auth/register", req, String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test @Order(3)
    void login_validCredentials_returnsTokens() {
        var req = new LoginRequest(EMAIL, PASSWORD);

        ResponseEntity<AuthResponse> resp =
                rest.postForEntity("/api/v1/auth/login", req, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().accessToken()).isNotBlank();
        accessToken  = resp.getBody().accessToken();
        refreshToken = resp.getBody().refreshToken();
    }

    @Test @Order(4)
    void login_wrongPassword_returns401() {
        var req = new LoginRequest(EMAIL, "wrongpassword");

        ResponseEntity<String> resp =
                rest.postForEntity("/api/v1/auth/login", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(5)
    void login_unknownEmail_returns401() {
        var req = new LoginRequest("nobody@ioteca.io", PASSWORD);

        ResponseEntity<String> resp =
                rest.postForEntity("/api/v1/auth/login", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── /me ───────────────────────────────────────────────────────────────────

    @Test @Order(6)
    void getMe_withoutToken_returns401() {
        ResponseEntity<String> resp =
                rest.getForEntity("/api/v1/auth/me", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(7)
    void getMe_withValidToken_returnsUserInfo() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<String> resp =
                rest.exchange("/api/v1/auth/me", HttpMethod.GET,
                        new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains(EMAIL);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test @Order(8)
    void refresh_validToken_returnsNewAccessToken() {
        var req = new TokenRefreshRequest(refreshToken);

        ResponseEntity<AuthResponse> resp =
                rest.postForEntity("/api/v1/auth/refresh", req, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().accessToken()).isNotBlank();
        assertThat(resp.getBody().accessToken()).isNotEqualTo(accessToken);
    }

    @Test @Order(9)
    void refresh_accessTokenInsteadOfRefresh_returns4xx() {
        var req = new TokenRefreshRequest(accessToken);

        ResponseEntity<String> resp =
                rest.postForEntity("/api/v1/auth/refresh", req, String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    // ── actuator ──────────────────────────────────────────────────────────────

    @Test @Order(10)
    void actuatorHealth_returns200() {
        ResponseEntity<String> resp =
                rest.getForEntity("/actuator/health", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
