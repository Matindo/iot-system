package io.ioteca.notification.service;

import io.ioteca.notification.config.PlatformConfig;
import io.ioteca.notification.entity.NotificationLog;
import io.ioteca.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock NotificationLogRepository logRepository;

    private EmailService emailService;

    private final UUID userId    = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, logRepository,
                new PlatformConfig("https://app.ioteca.io", "noreply@ioteca.io"));
    }

    // ── send happy path ───────────────────────────────────────────────────────

    @Test
    void send_invokesMailSender() {
when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        emailService.send(userId, projectId, "ALERT", "to@test.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).contains("to@test.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Subject");
    }

    @Test
    void send_logsDeliveredTrue_onSuccess() {
ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        when(logRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        emailService.send(userId, projectId, "ALERT", "to@test.com", "Subject", "Body");

        assertThat(captor.getValue().isDelivered()).isTrue();
        assertThat(captor.getValue().getError()).isNull();
    }

    // ── send failure ──────────────────────────────────────────────────────────

    @Test
    void send_logsError_whenMailSenderThrows() {
doThrow(new RuntimeException("SMTP timeout")).when(mailSender).send(any(SimpleMailMessage.class));
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        when(logRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        emailService.send(userId, projectId, "ALERT", "to@test.com", "Subject", "Body");

        assertThat(captor.getValue().isDelivered()).isFalse();
        assertThat(captor.getValue().getError()).contains("SMTP timeout");
    }

    @Test
    void send_alwaysSavesLog_evenOnFailure() {
doThrow(new RuntimeException("fail")).when(mailSender).send(any(SimpleMailMessage.class));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        emailService.send(userId, projectId, "ALERT", "to@test.com", "Subject", "Body");

        verify(logRepository).save(any());
    }

    // ── wasSentRecently ───────────────────────────────────────────────────────

    @Test
    void wasSentRecently_delegatesToRepository_true() {
        when(logRepository.existsByProjectIdAndTypeAndSentAtAfter(eq(projectId), eq("QUOTA_WARNING"), any()))
                .thenReturn(true);

        assertThat(emailService.wasSentRecently(projectId, "QUOTA_WARNING", 60)).isTrue();
    }

    @Test
    void wasSentRecently_delegatesToRepository_false() {
        when(logRepository.existsByProjectIdAndTypeAndSentAtAfter(eq(projectId), eq("QUOTA_WARNING"), any()))
                .thenReturn(false);

        assertThat(emailService.wasSentRecently(projectId, "QUOTA_WARNING", 60)).isFalse();
    }

    @Test
    void wasSentRecently_passesCorrectCutoffTime() {
        ArgumentCaptor<OffsetDateTime> timeCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        when(logRepository.existsByProjectIdAndTypeAndSentAtAfter(any(), any(), timeCaptor.capture()))
                .thenReturn(false);

        OffsetDateTime before = OffsetDateTime.now().minusMinutes(31);
        emailService.wasSentRecently(projectId, "ALERT", 30);
        OffsetDateTime after  = OffsetDateTime.now().minusMinutes(29);

        assertThat(timeCaptor.getValue()).isBetween(before, after);
    }
}
