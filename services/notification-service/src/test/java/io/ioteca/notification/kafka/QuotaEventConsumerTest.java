package io.ioteca.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.notification.entity.Project;
import io.ioteca.notification.entity.User;
import io.ioteca.notification.repository.ProjectRepository;
import io.ioteca.notification.repository.UserRepository;
import io.ioteca.notification.service.EmailService;
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
class QuotaEventConsumerTest {

    @Mock EmailService emailService;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock Acknowledgment ack;

    @InjectMocks QuotaEventConsumer consumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID projectId;
    private UUID userId;
    private Project project;
    private User owner;

    @BeforeEach
    void setUp() throws Exception {
        var objMapperField = QuotaEventConsumer.class.getDeclaredField("objectMapper");
        objMapperField.setAccessible(true);
        objMapperField.set(consumer, objectMapper);

        var baseUrlField = QuotaEventConsumer.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(consumer, "https://app.ioteca.io");

        projectId = UUID.randomUUID();
        userId    = UUID.randomUUID();

        project = new Project();
        project.setId(projectId);
        project.setUserId(userId);

        owner = new User();
        owner.setId(userId);
        owner.setEmail("owner@ioteca.io");
        owner.setFullName("Test User");
    }

    // ── WARNING_80 ────────────────────────────────────────────────────────────

    @Test
    void consume_sendsWarningEmail_onWarning80WhenNotSentRecently() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(emailService.wasSentRecently(projectId, "QUOTA_WARNING", 60)).thenReturn(false);

        consume("WARNING_80");

        verify(emailService).send(any(), eq(projectId), eq("QUOTA_WARNING"), any(), any(), any());
    }

    @Test
    void consume_skipsWarningEmail_whenSentRecently() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(emailService.wasSentRecently(projectId, "QUOTA_WARNING", 60)).thenReturn(true);

        consume("WARNING_80");

        verify(emailService, never()).send(any(), any(), eq("QUOTA_WARNING"), any(), any(), any());
    }

    // ── EXCEEDED ──────────────────────────────────────────────────────────────

    @Test
    void consume_sendsExceededEmail_onExceededWhenNotSentRecently() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(emailService.wasSentRecently(projectId, "QUOTA_EXCEEDED", 60)).thenReturn(false);

        consume("EXCEEDED");

        verify(emailService).send(any(), eq(projectId), eq("QUOTA_EXCEEDED"), any(), any(), any());
    }

    @Test
    void consume_skipsExceededEmail_whenSentRecently() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(emailService.wasSentRecently(projectId, "QUOTA_EXCEEDED", 60)).thenReturn(true);

        consume("EXCEEDED");

        verify(emailService, never()).send(any(), any(), eq("QUOTA_EXCEEDED"), any(), any(), any());
    }

    // ── email address ─────────────────────────────────────────────────────────

    @Test
    void consume_usesProjectAlertEmail_whenSet() throws Exception {
        project.setAlertEmail("alerts@project.com");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(emailService.wasSentRecently(projectId, "QUOTA_WARNING", 60)).thenReturn(false);

        consume("WARNING_80");

        verify(emailService).send(any(), any(), any(), eq("alerts@project.com"), any(), any());
    }

    @Test
    void consume_fallsBackToOwnerEmail_whenNoProjectAlertEmail() throws Exception {
        project.setAlertEmail(null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(emailService.wasSentRecently(projectId, "QUOTA_WARNING", 60)).thenReturn(false);

        consume("WARNING_80");

        verify(emailService).send(any(), any(), any(), eq("owner@ioteca.io"), any(), any());
    }

    // ── early returns ─────────────────────────────────────────────────────────

    @Test
    void consume_acksAndReturns_whenProjectNotFound() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        consume("WARNING_80");

        verify(ack).acknowledge();
        verify(emailService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_acksAndReturns_whenUserNotFound() throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        consume("WARNING_80");

        verify(ack).acknowledge();
        verify(emailService, never()).send(any(), any(), any(), any(), any(), any());
    }

    // ── resilience ────────────────────────────────────────────────────────────

    @Test
    void consume_acksEvenOnParseException() throws Exception {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("quota.events", 0, 0L, "key", "not-json");

        consumer.consume(record, ack);

        verify(ack).acknowledge();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void consume(String status) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "status", status,
                "projectId", projectId.toString(),
                "count", 8000,
                "limit", 10000
        ));
        consumer.consume(new ConsumerRecord<>("quota.events", 0, 0L, projectId.toString(), json), ack);
    }
}
