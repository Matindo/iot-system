package io.ioteca.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.notification.entity.Project;
import io.ioteca.notification.entity.User;
import io.ioteca.notification.repository.NotificationLogRepository;
import io.ioteca.notification.repository.ProjectRepository;
import io.ioteca.notification.repository.UserRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationServiceIT {

    @MockBean JavaMailSender mailSender;

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired NotificationLogRepository notificationLogRepository;
    @Autowired KafkaListenerEndpointRegistry endpointRegistry;

    private UUID projectId;
    private UUID userId;

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

        notificationLogRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setId(userId);
        user.setEmail("owner@test.io");
        user.setFullName("Test Owner");
        userRepository.save(user);

        Project project = new Project();
        project.setId(projectId);
        project.setUserId(userId);
        project.setAlertEmail("alerts@test.io");
        projectRepository.save(project);
    }

    @Test
    void alertEvent_emailChannel_createsNotificationLog() throws Exception {
        var event = Map.of(
                "ruleId", UUID.randomUUID().toString(),
                "projectId", projectId.toString(),
                "deviceId", "sensor-01",
                "metricName", "temperature",
                "actualValue", 35.5,
                "condition", "gt",
                "threshold", 30.0,
                "firedAt", "2024-01-01T00:00:00Z",
                "notificationChannels", List.of("email")
        );
        kafkaTemplate.send("alert.events", projectId.toString(), objectMapper.writeValueAsString(event));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var logs = notificationLogRepository.findAll();
            assertThat(logs).anyMatch(log ->
                    "ALERT".equals(log.getType()) &&
                    "EMAIL".equals(log.getChannel()) &&
                    projectId.equals(log.getProjectId()));
        });
    }

    @Test
    void alertEvent_unknownProject_isDropped() throws Exception {
        UUID unknownProjectId = UUID.randomUUID();
        var event = Map.of(
                "ruleId", UUID.randomUUID().toString(),
                "projectId", unknownProjectId.toString(),
                "deviceId", "sensor-01",
                "metricName", "temperature",
                "actualValue", 35.5,
                "condition", "gt",
                "threshold", 30.0,
                "firedAt", "2024-01-01T00:00:00Z",
                "notificationChannels", List.of("email")
        );
        kafkaTemplate.send("alert.events", unknownProjectId.toString(), objectMapper.writeValueAsString(event));

        Thread.sleep(5_000);
        assertThat(notificationLogRepository.findAll()).isEmpty();
    }

    @Test
    void quotaExceededEvent_createsNotificationLog() throws Exception {
        var event = Map.of(
                "projectId", projectId.toString(),
                "status", "EXCEEDED",
                "count", 1000,
                "limit", 1000
        );
        kafkaTemplate.send("quota.events", projectId.toString(), objectMapper.writeValueAsString(event));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var logs = notificationLogRepository.findAll();
            assertThat(logs).anyMatch(log ->
                    "QUOTA_EXCEEDED".equals(log.getType()) &&
                    "EMAIL".equals(log.getChannel()) &&
                    projectId.equals(log.getProjectId()));
        });
    }

    @Test
    void quotaWarningEvent_createsNotificationLog() throws Exception {
        var event = Map.of(
                "projectId", projectId.toString(),
                "status", "WARNING_80",
                "count", 800,
                "limit", 1000
        );
        kafkaTemplate.send("quota.events", projectId.toString(), objectMapper.writeValueAsString(event));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var logs = notificationLogRepository.findAll();
            assertThat(logs).anyMatch(log ->
                    "QUOTA_WARNING".equals(log.getType()) &&
                    "EMAIL".equals(log.getChannel()) &&
                    projectId.equals(log.getProjectId()));
        });
    }

    @Test
    void alertEmailDelivered_logMarkedDelivered() throws Exception {
        var event = Map.of(
                "ruleId", UUID.randomUUID().toString(),
                "projectId", projectId.toString(),
                "deviceId", "sensor-01",
                "metricName", "co2",
                "actualValue", 1500.0,
                "condition", "gt",
                "threshold", 1000.0,
                "firedAt", "2024-01-01T00:00:00Z",
                "notificationChannels", List.of("email")
        );
        kafkaTemplate.send("alert.events", projectId.toString(), objectMapper.writeValueAsString(event));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var logs = notificationLogRepository.findAll();
            assertThat(logs).anyMatch(log ->
                    "ALERT".equals(log.getType()) && log.isDelivered());
        });
    }
}
