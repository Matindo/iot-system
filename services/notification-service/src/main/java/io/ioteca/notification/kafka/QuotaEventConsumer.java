package io.ioteca.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.notification.entity.Project;
import io.ioteca.notification.entity.User;
import io.ioteca.notification.repository.ProjectRepository;
import io.ioteca.notification.repository.UserRepository;
import io.ioteca.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaEventConsumer {

    private final EmailService emailService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${platform.base-url}")
    private String baseUrl;

    @KafkaListener(topics = "quota.events", groupId = "notification-service-group")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String status    = (String) event.get("status");
            UUID projectId   = UUID.fromString((String) event.get("projectId"));
            long count       = ((Number) event.get("count")).longValue();
            long limit       = ((Number) event.get("limit")).longValue();

            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) { ack.acknowledge(); return; }

            User owner = userRepository.findById(project.getUserId()).orElse(null);
            if (owner == null) { ack.acknowledge(); return; }

            String toEmail = project.getAlertEmail() != null
                    ? project.getAlertEmail() : owner.getEmail();

            if ("WARNING_80".equals(status) && !emailService.wasSentRecently(projectId, "QUOTA_WARNING", 60)) {
                String subject = "IoTeca: You've used 80% of your daily quota";
                String body = String.format(
                        "Hi %s,\n\nYour project has used %d of %d daily messages (80%%).\n" +
                        "Consider upgrading your plan at %s/settings/billing.\n\nIoTeca Team",
                        owner.getFullName() != null ? owner.getFullName() : owner.getEmail(),
                        count, limit, baseUrl);
                emailService.send(owner.getId(), projectId, "QUOTA_WARNING", toEmail, subject, body);
            }

            if ("EXCEEDED".equals(status) && !emailService.wasSentRecently(projectId, "QUOTA_EXCEEDED", 60)) {
                String subject = "IoTeca: Daily quota exceeded â€” messages are being rejected";
                String body = String.format(
                        "Hi %s,\n\nYour project has exceeded its daily limit of %d messages.\n" +
                        "New messages are now rejected until midnight (UTC) or until you upgrade.\n\n" +
                        "Upgrade now: %s/settings/billing\n\nIoTeca Team",
                        owner.getFullName() != null ? owner.getFullName() : owner.getEmail(),
                        limit, baseUrl);
                emailService.send(owner.getId(), projectId, "QUOTA_EXCEEDED", toEmail, subject, body);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process quota event: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
