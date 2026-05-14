package io.ioteca.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayIT {

    @MockBean KafkaTemplate<String, String> kafkaTemplate;
    @MockBean StringRedisTemplate stringRedisTemplate;

    @Autowired TestRestTemplate rest;

    private static final String JWT_SECRET = "test-secret-key-minimum-32-chars!!";
    private UUID testUserId;
    private String authHeader;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        authHeader = "Bearer " + buildJwt(testUserId, "USER");

        var ops = org.mockito.Mockito.mock(
                org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(null);
    }

    // ── health ────────────────────────────────────────────────────────────────

    @Test
    void actuatorHealth_isUp() {
        ResponseEntity<String> resp = rest.getForEntity("/actuator/health", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("UP");
    }

    // ── project management ────────────────────────────────────────────────────

    @Test
    void listProjects_withoutAuth_returns401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/projects", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createProject_returnsCreatedProject() {
        var body = Map.of("name", "Test Project", "region", "af-ke-1");

        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(authHeader)), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("Test Project");
    }

    @Test
    void listProjects_withAuth_returnsOwnProjects() {
        var body = Map.of("name", "My Sensor Grid");
        rest.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(authHeader)), String.class);

        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/projects", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(authHeader)), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("My Sensor Grid");
    }

    @Test
    void listProjects_doesNotReturnOtherUsersProjects() {
        String otherAuth = "Bearer " + buildJwt(UUID.randomUUID(), "USER");
        rest.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Other User Project"), jsonHeaders(otherAuth)), String.class);

        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/projects", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(authHeader)), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).doesNotContain("Other User Project");
    }

    @Test
    void getProject_notOwned_returns403or404() {
        ResponseEntity<String> created = rest.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Private Project"), jsonHeaders(authHeader)), String.class);
        String projectId = extractId(created.getBody());

        String otherAuth = "Bearer " + buildJwt(UUID.randomUUID(), "USER");
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(otherAuth)), String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void deleteProject_softDeletesIt() {
        ResponseEntity<String> created = rest.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "To Delete"), jsonHeaders(authHeader)), String.class);
        String projectId = extractId(created.getBody());

        ResponseEntity<Void> del = rest.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders(authHeader)), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> list = rest.exchange(
                "/api/v1/projects", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(authHeader)), String.class);
        assertThat(list.getBody()).doesNotContain("To Delete");
    }

    // ── ingest ────────────────────────────────────────────────────────────────

    @Test
    void ingest_withJwtAndProjectId_returns202() {
        UUID projectId = UUID.randomUUID();
        var body = Map.of(
                "device_id", "sensor-01",
                "metrics", Map.of("temperature", 24.5)
        );

        ResponseEntity<Void> resp = rest.exchange(
                "/api/v1/ingest?projectId=" + projectId, HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(authHeader)), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void ingest_withoutAuth_returns401() {
        var body = Map.of("device_id", "sensor-01", "metrics", Map.of("temperature", 24.5));

        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/ingest?projectId=" + UUID.randomUUID(), HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(null)), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildJwt(UUID userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", "test@ioteca.io")
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    private HttpHeaders jsonHeaders(String bearerToken) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) h.set("Authorization", bearerToken);
        return h;
    }

    private String extractId(String json) {
        int idx = json.indexOf("\"id\":\"") + 6;
        return json.substring(idx, json.indexOf("\"", idx));
    }
}
