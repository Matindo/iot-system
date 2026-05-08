package io.ioteca.notification.service;

import io.ioteca.notification.config.PlatformConfig;
import io.ioteca.notification.entity.NotificationLog;
import io.ioteca.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository logRepository;
    private final PlatformConfig platformConfig;

    public void send(UUID userId, UUID projectId, String type,
                     String toEmail, String subject, String body) {
        NotificationLog entry = NotificationLog.builder()
                .userId(userId)
                .projectId(projectId)
                .type(type)
                .channel("EMAIL")
                .subject(subject)
                .body(body)
                .build();
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(platformConfig.fromEmail());
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            entry.setDelivered(true);
            log.info("Email sent: type={} to={}", type, toEmail);
        } catch (Exception e) {
            entry.setError(e.getMessage());
            log.error("Email failed: type={} to={} error={}", type, toEmail, e.getMessage());
        } finally {
            logRepository.save(entry);
        }
    }

    public boolean wasSentRecently(UUID projectId, String type, int withinMinutes) {
        return logRepository.existsByProjectIdAndTypeAndSentAtAfter(
                projectId, type, OffsetDateTime.now().minusMinutes(withinMinutes));
    }
}
