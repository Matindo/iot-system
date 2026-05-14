package io.ioteca.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.alert.entity.AlertRule;
import io.ioteca.alert.repository.AlertRuleRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlertEngineIT {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired AlertRuleRepository alertRuleRepository;
    @Autowired AlertCapture alertCapture;
    @Autowired KafkaListenerEndpointRegistry endpointRegistry;

    private static final String PROCESSED_TOPIC = "processed.ingest.af-ke-1";
    private UUID projectId;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AlertCapture alertCapture() { return new AlertCapture(); }
    }

    static class AlertCapture {
        final LinkedBlockingQueue<String> events = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "alert.events", groupId = "alert-it-capture")
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
        alertCapture.events.clear();
        alertRuleRepository.deleteAll();
    }

    @Test
    void metricExceedsThreshold_alertFired() throws Exception {
        AlertRule rule = AlertRule.builder()
                .projectId(projectId)
                .name("High temperature")
                .metricName("temperature")
                .condition("gt")
                .threshold(30.0)
                .notificationChannels(new String[]{"email"})
                .isActive(true)
                .build();
        alertRuleRepository.save(rule);

        var msg = processedMsg(projectId, "sensor-01", Map.of("temperature", 35.0));
        kafkaTemplate.send(PROCESSED_TOPIC, projectId.toString(), objectMapper.writeValueAsString(msg));

        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() ->
                alertCapture.events.stream().anyMatch(e -> e.contains(projectId.toString()))
        );
        String event = alertCapture.events.stream()
                .filter(e -> e.contains(projectId.toString()))
                .findFirst().orElseThrow();
        assertThat(event).contains("temperature").containsAnyOf("35.0", "35");
    }

    @Test
    void metricBelowThreshold_noAlertFired() throws Exception {
        AlertRule rule = AlertRule.builder()
                .projectId(projectId)
                .name("High temp")
                .metricName("temperature")
                .condition("gt")
                .threshold(30.0)
                .notificationChannels(new String[]{"email"})
                .isActive(true)
                .build();
        alertRuleRepository.save(rule);

        var msg = processedMsg(projectId, "sensor-01", Map.of("temperature", 25.0));
        kafkaTemplate.send(PROCESSED_TOPIC, projectId.toString(), objectMapper.writeValueAsString(msg));

        Thread.sleep(5_000);
        assertThat(alertCapture.events.stream().anyMatch(e -> e.contains(projectId.toString()))).isFalse();
    }

    @Test
    void inSuppressionWindow_alertSuppressed() throws Exception {
        AlertRule rule = AlertRule.builder()
                .projectId(projectId)
                .name("Suppressed rule")
                .metricName("pressure")
                .condition("gt")
                .threshold(100.0)
                .suppressionWindowSeconds(3600)
                .lastFiredAt(OffsetDateTime.now().minusMinutes(5))
                .notificationChannels(new String[]{"email"})
                .isActive(true)
                .build();
        alertRuleRepository.save(rule);

        var msg = processedMsg(projectId, "pressure-sensor", Map.of("pressure", 150.0));
        kafkaTemplate.send(PROCESSED_TOPIC, projectId.toString(), objectMapper.writeValueAsString(msg));

        Thread.sleep(5_000);
        assertThat(alertCapture.events.stream().anyMatch(e -> e.contains(projectId.toString()))).isFalse();
    }

    @Test
    void noMatchingRule_noAlertFired() throws Exception {
        AlertRule rule = AlertRule.builder()
                .projectId(projectId)
                .name("Humidity rule")
                .metricName("humidity")
                .condition("gt")
                .threshold(90.0)
                .notificationChannels(new String[]{"email"})
                .isActive(true)
                .build();
        alertRuleRepository.save(rule);

        var msg = processedMsg(projectId, "sensor-01", Map.of("temperature", 50.0));
        kafkaTemplate.send(PROCESSED_TOPIC, projectId.toString(), objectMapper.writeValueAsString(msg));

        Thread.sleep(5_000);
        assertThat(alertCapture.events.stream().anyMatch(e -> e.contains(projectId.toString()))).isFalse();
    }

    @Test
    void lteCondition_metricAtThreshold_alertFired() throws Exception {
        AlertRule rule = AlertRule.builder()
                .projectId(projectId)
                .name("Low battery")
                .metricName("battery")
                .condition("lte")
                .threshold(20.0)
                .notificationChannels(new String[]{"email", "webhook"})
                .isActive(true)
                .build();
        alertRuleRepository.save(rule);

        var msg = processedMsg(projectId, "sensor-01", Map.of("battery", 20.0));
        kafkaTemplate.send(PROCESSED_TOPIC, projectId.toString(), objectMapper.writeValueAsString(msg));

        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() ->
                alertCapture.events.stream().anyMatch(e -> e.contains(projectId.toString()))
        );
        String event = alertCapture.events.stream()
                .filter(e -> e.contains(projectId.toString()))
                .findFirst().orElseThrow();
        assertThat(event).contains("battery").contains(projectId.toString());
    }

    private Map<String, Object> processedMsg(UUID pid, String deviceId, Map<String, Object> metrics) {
        return Map.of(
                "projectId", pid.toString(),
                "deviceId", deviceId,
                "time", OffsetDateTime.now().toString(),
                "ingestedAt", OffsetDateTime.now().toString(),
                "metrics", metrics,
                "tags", Map.of()
        );
    }
}
