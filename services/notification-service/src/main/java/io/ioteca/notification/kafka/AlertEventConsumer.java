package io.ioteca.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.notification.entity.Project;
import io.ioteca.notification.entity.User;
import io.ioteca.notification.repository.ProjectRepository;
import io.ioteca.notification.repository.UserRepository;
import io.ioteca.notification.service.EmailService;
import io.ioteca.notification.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEventConsumer {

    private final EmailService emailService;
    private final WebhookService webhookService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "alert.events", groupId = "notification-service-group")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            UUID projectId  = UUID.fromString((String) event.get("projectId"));
            String ruleId   = (String) event.get("ruleId");
            String deviceId = (String) event.get("deviceId");
            String metric   = (String) event.get("metricName");
            Object value    = event.get("actualValue");
            String condition = (String) event.get("condition");
            Object threshold = event.get("threshold");

            @SuppressWarnings("unchecked")
            String[] channels = objectMapper.convertValue(
                    event.get("notificationChannels"), String[].class);

            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) { ack.acknowledge(); return; }

            User owner = userRepository.findById(project.getUserId()).orElse(null);
            if (owner == null) { ack.acknowledge(); return; }

            boolean notifyEmail   = Arrays.asList(channels).contains("email");
            boolean notifyWebhook = Arrays.asList(channels).contains("webhook");

            if (notifyEmail) {
                String toEmail = project.getAlertEmail() != null
                        ? project.getAlertEmail() : owner.getEmail();
                String subject = String.format("Alert: %s on device %s", metric, deviceId);
                String body = String.format(
                        "Alert rule %s triggered.\nDevice: %s\nMetric: %s\nValue: %s %s %s",
                        ruleId, deviceId, metric, value, condition, threshold);
                emailService.send(owner.getId(), projectId, "ALERT", toEmail, subject, body);
            }

            if (notifyWebhook && project.getAlertWebhookUrl() != null) {
                webhookService.post(project.getAlertWebhookUrl(), event);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process alert event: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
