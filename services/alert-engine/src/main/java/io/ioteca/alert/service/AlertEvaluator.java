package io.ioteca.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.alert.dto.AlertEvent;
import io.ioteca.alert.dto.ProcessedMessage;
import io.ioteca.alert.entity.AlertRule;
import io.ioteca.alert.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEvaluator {

    private static final String ALERT_EVENTS_TOPIC = "alert.events";

    private final AlertRuleRepository alertRuleRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Transactional
    public void evaluate(ProcessedMessage msg) {
        if (msg.metrics() == null) return;

        for (Map.Entry<String, Object> entry : msg.metrics().entrySet()) {
            String metricName = entry.getKey();
            Object rawValue = entry.getValue();

            if (!(rawValue instanceof Number)) continue;
            double value = ((Number) rawValue).doubleValue();

            List<AlertRule> rules = alertRuleRepository.findActiveRulesForMetric(
                    msg.projectId(), metricName, msg.deviceId());

            for (AlertRule rule : rules) {
                if (isInSuppressionWindow(rule)) continue;
                if (conditionMet(rule, value)) {
                    fireAlert(rule, metricName, value, msg);
                }
            }
        }
    }

    private boolean conditionMet(AlertRule rule, double value) {
        if (rule.getThreshold() == null) return false;
        double threshold = rule.getThreshold();
        return switch (rule.getCondition()) {
            case "gt"  -> value > threshold;
            case "gte" -> value >= threshold;
            case "lt"  -> value < threshold;
            case "lte" -> value <= threshold;
            case "eq"  -> Double.compare(value, threshold) == 0;
            default    -> false;
        };
    }

    private boolean isInSuppressionWindow(AlertRule rule) {
        if (rule.getLastFiredAt() == null || rule.getSuppressionWindowSeconds() == null) return false;
        return rule.getLastFiredAt()
                .plusSeconds(rule.getSuppressionWindowSeconds())
                .isAfter(OffsetDateTime.now());
    }

    private void fireAlert(AlertRule rule, String metricName, double value, ProcessedMessage msg) {
        OffsetDateTime now = OffsetDateTime.now();
        rule.setLastFiredAt(now);
        alertRuleRepository.save(rule);

        AlertEvent event = new AlertEvent(
                rule.getId(),
                rule.getProjectId(),
                msg.deviceId(),
                metricName,
                value,
                rule.getCondition(),
                rule.getThreshold(),
                now,
                rule.getNotificationChannels()
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ALERT_EVENTS_TOPIC, rule.getProjectId().toString(), json);
            log.info("Alert fired: rule={} project={} device={} metric={} value={}",
                    rule.getId(), rule.getProjectId(), msg.deviceId(), metricName, value);
        } catch (Exception e) {
            log.error("Failed to emit alert event for rule {}: {}", rule.getId(), e.getMessage());
        }
    }
}
