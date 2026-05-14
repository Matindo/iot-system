package io.ioteca.quota;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuotaServiceIT {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired StringRedisTemplate redis;
    @Autowired JdbcTemplate jdbc;
    @Autowired QuotaCapture quotaCapture;
    @Autowired KafkaListenerEndpointRegistry endpointRegistry;

    private static final String RAW_TOPIC = "raw.ingest.af-ke-1";
    private UUID projectId;
    private UUID userId;

    @TestConfiguration
    static class TestConfig {
        @Bean
        QuotaCapture quotaCapture() { return new QuotaCapture(); }
    }

    static class QuotaCapture {
        final LinkedBlockingQueue<String> events = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "quota.events", groupId = "quota-it-capture")
        void capture(String msg) { events.offer(msg); }
    }

    @BeforeAll
    void waitForKafkaConsumers() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> endpointRegistry.getListenerContainers().stream()
                        .filter(c -> c instanceof ConcurrentMessageListenerContainer)
                        .map(c -> (ConcurrentMessageListenerContainer<?, ?>) c)
                        .allMatch(c -> !c.getAssignedPartitions().isEmpty()));
    }

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        quotaCapture.events.clear();

        redis.delete("project:" + projectId + ":msgs:" + LocalDate.now());

        jdbc.update("DELETE FROM platform.user_subscriptions");
        jdbc.update("DELETE FROM platform.projects");
        jdbc.update("DELETE FROM platform.subscription_tiers");

        Integer tierId = jdbc.queryForObject(
                "INSERT INTO platform.subscription_tiers " +
                "(name, max_messages_per_day, max_message_rate_per_sec, storage_limit_mb) " +
                "VALUES ('TEST', 1, 10, 100) RETURNING id",
                Integer.class);

        jdbc.update(
                "INSERT INTO platform.projects (id, user_id, is_active) VALUES (?, ?, ?)",
                projectId, userId, true);

        jdbc.update(
                "INSERT INTO platform.user_subscriptions (id, user_id, tier_id, status, expires_at) " +
                "VALUES (?, ?, ?, 'ACTIVE', ?)",
                UUID.randomUUID(), userId, tierId, OffsetDateTime.now().plusDays(30));
    }

    @Test
    void message_exceedsDailyLimit_quotaEventEmitted() throws Exception {
        var raw = Map.of(
                "device_id", "sensor-01",
                "project_id", projectId.toString(),
                "metrics", Map.of("temperature", 25.0)
        );
        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), objectMapper.writeValueAsString(raw));

        String event = quotaCapture.events.poll(20, TimeUnit.SECONDS);
        assertThat(event).isNotNull()
                .contains(projectId.toString())
                .contains("EXCEEDED");
    }

    @Test
    void message_withinLimit_noQuotaEvent() throws Exception {
        jdbc.update("DELETE FROM platform.user_subscriptions");
        jdbc.update("DELETE FROM platform.subscription_tiers");

        Integer tierId = jdbc.queryForObject(
                "INSERT INTO platform.subscription_tiers " +
                "(name, max_messages_per_day, max_message_rate_per_sec, storage_limit_mb) " +
                "VALUES ('FREE', 10000, 100, 5000) RETURNING id",
                Integer.class);

        jdbc.update(
                "INSERT INTO platform.user_subscriptions (id, user_id, tier_id, status, expires_at) " +
                "VALUES (?, ?, ?, 'ACTIVE', ?)",
                UUID.randomUUID(), userId, tierId, OffsetDateTime.now().plusDays(30));

        var raw = Map.of(
                "device_id", "sensor-01",
                "project_id", projectId.toString(),
                "metrics", Map.of("temperature", 25.0)
        );
        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), objectMapper.writeValueAsString(raw));

        Thread.sleep(5_000);
        assertThat(quotaCapture.events).isEmpty();
    }

    @Test
    void redisCounter_incrementedOnEachMessage() throws Exception {
        jdbc.update("DELETE FROM platform.user_subscriptions");
        jdbc.update("DELETE FROM platform.subscription_tiers");

        Integer tierId = jdbc.queryForObject(
                "INSERT INTO platform.subscription_tiers " +
                "(name, max_messages_per_day, max_message_rate_per_sec, storage_limit_mb) " +
                "VALUES ('FREE', 10000, 100, 5000) RETURNING id",
                Integer.class);

        jdbc.update(
                "INSERT INTO platform.user_subscriptions (id, user_id, tier_id, status, expires_at) " +
                "VALUES (?, ?, ?, 'ACTIVE', ?)",
                UUID.randomUUID(), userId, tierId, OffsetDateTime.now().plusDays(30));

        var raw = Map.of(
                "device_id", "sensor-01",
                "project_id", projectId.toString(),
                "metrics", Map.of("temperature", 25.0)
        );
        String payload = objectMapper.writeValueAsString(raw);

        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), payload);
        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), payload);
        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), payload);

        String redisKey = "project:" + projectId + ":msgs:" + LocalDate.now();
        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            String val = redis.opsForValue().get(redisKey);
            assertThat(val).isNotNull();
            assertThat(Long.parseLong(val)).isGreaterThanOrEqualTo(3);
        });
    }

    @Test
    void warningThreshold_quotaEventWithWarningStatus() throws Exception {
        jdbc.update("DELETE FROM platform.user_subscriptions");
        jdbc.update("DELETE FROM platform.subscription_tiers");

        Integer tierId = jdbc.queryForObject(
                "INSERT INTO platform.subscription_tiers " +
                "(name, max_messages_per_day, max_message_rate_per_sec, storage_limit_mb) " +
                "VALUES ('SMALL', 10, 100, 1000) RETURNING id",
                Integer.class);

        jdbc.update(
                "INSERT INTO platform.user_subscriptions (id, user_id, tier_id, status, expires_at) " +
                "VALUES (?, ?, ?, 'ACTIVE', ?)",
                UUID.randomUUID(), userId, tierId, OffsetDateTime.now().plusDays(30));

        var raw = Map.of(
                "device_id", "sensor-01",
                "project_id", projectId.toString(),
                "metrics", Map.of("temperature", 25.0)
        );
        String payload = objectMapper.writeValueAsString(raw);

        for (int i = 0; i < 8; i++) {
            kafkaTemplate.send(RAW_TOPIC, projectId.toString(), payload);
        }

        Awaitility.await().atMost(Duration.ofSeconds(30)).until(() ->
                quotaCapture.events.stream().anyMatch(e -> e.contains("WARNING_80"))
        );
    }
}
