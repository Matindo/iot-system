package io.ioteca.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.notification.entity.Project;
import io.ioteca.notification.entity.User;
import io.ioteca.notification.repository.ProjectRepository;
import io.ioteca.notification.repository.UserRepository;
import io.ioteca.notification.service.EmailService;
import io.ioteca.notification.service.WebhookService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEventConsumerTest {

    @Mock EmailService emailService;
    @Mock WebhookService webhookService;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock Acknowledgment ack;

    @InjectMocks AlertEventConsumer consumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID projectId;
    private UUID userId;
    private Project project;
    private User owner;

    @BeforeEach
    void setUp() throws Exception {
        var field = AlertEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, objectMapper);

        projectId = UUID.randomUUID();
        userId    = UUID.randomUUID();

        project = new Project();
        project.setId(projectId);
        project.setUserId(userId);

        owner = new User();
        owner.setId(userId);
        owner.setEmail("owner@ioteca.io");
    }

    // ── channel routing ───────────────────────────────────────────────────────

    @Test
    void consume_sendsEmail_whenEmailChannel() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));

        consume(eventJson(projectId, new String[]{"email"}));

        verify(emailService).send(eq(userId), eq(projectId), eq("ALERT"), any(), any(), any());
        verify(webhookService, never()).post(any(), any());
    }

    @Test
    void consume_sendsWebhook_whenWebhookChannelAndUrlSet() throws Exception {
        project.setAlertWebhookUrl("https://example.com/hook");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));

        consume(eventJson(projectId, new String[]{"webhook"}));

        verify(webhookService).post(eq("https://example.com/hook"), any());
        verify(emailService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_skipWebhook_whenWebhookChannelButNoUrl() throws Exception {
        project.setAlertWebhookUrl(null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));

        consume(eventJson(projectId, new String[]{"webhook"}));

        verify(webhookService, never()).post(any(), any());
    }

    @Test
    void consume_sendsBothEmailAndWebhook_whenBothChannels() throws Exception {
        project.setAlertWebhookUrl("https://example.com/hook");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));

        consume(eventJson(projectId, new String[]{"email", "webhook"}));

        verify(emailService).send(any(), any(), any(), any(), any(), any());
        verify(webhookService).post(any(), any());
    }

    // ── email address resolution ──────────────────────────────────────────────

    @Test
    void consume_usesProjectAlertEmail_whenSet() throws Exception {
        project.setAlertEmail("alerts@project.com");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));

        consume(eventJson(projectId, new String[]{"email"}));

        verify(emailService).send(any(), any(), any(), eq("alerts@project.com"), any(), any());
    }

    @Test
    void consume_fallsBackToOwnerEmail_whenNoProjectAlertEmail() throws Exception {
        project.setAlertEmail(null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));

        consume(eventJson(projectId, new String[]{"email"}));

        verify(emailService).send(any(), any(), any(), eq("owner@ioteca.io"), any(), any());
    }

    // ── early returns ─────────────────────────────────────────────────────────

    @Test
    void consume_acksAndReturns_whenProjectNotFound() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        consume(eventJson(projectId, new String[]{"email"}));

        verify(ack).acknowledge();
        verify(emailService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_acksAndReturns_whenUserNotFound() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        consume(eventJson(projectId, new String[]{"email"}));

        verify(ack).acknowledge();
        verify(emailService, never()).send(any(), any(), any(), any(), any(), any());
    }

    // ── resilience ────────────────────────────────────────────────────────────

    @Test
    void consume_acksEvenOnParseException() throws Exception {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("alert.events", 0, 0L, "key", "not-valid-json");

        consumer.consume(record, ack);

        verify(ack).acknowledge();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void consume(String json) throws Exception {
        consumer.consume(new ConsumerRecord<>("alert.events", 0, 0L, projectId.toString(), json), ack);
    }

    private String eventJson(UUID projectId, String[] channels) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "projectId", projectId.toString(),
                "ruleId", UUID.randomUUID().toString(),
                "deviceId", "sensor_01",
                "metricName", "temperature",
                "actualValue", 35.0,
                "condition", "gt",
                "threshold", 30.0,
                "notificationChannels", channels
        ));
    }
}
