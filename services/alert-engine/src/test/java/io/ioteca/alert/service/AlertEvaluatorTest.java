package io.ioteca.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.alert.dto.ProcessedMessage;
import io.ioteca.alert.entity.AlertRule;
import io.ioteca.alert.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEvaluatorTest {

    @Mock AlertRuleRepository alertRuleRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock RestTemplate restTemplate;
    @Mock ObjectMapper objectMapper;

    @InjectMocks AlertEvaluator alertEvaluator;

    private UUID projectId;
    private ProcessedMessage msg;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        msg = new ProcessedMessage(projectId, "sensor_01",
                OffsetDateTime.now(), OffsetDateTime.now(),
                Map.of("temperature", 30.0, "humidity", 65.0), Map.of());
    }

    // ── condition evaluation ──────────────────────────────────────────────────

    @Test
    void gt_firesWhenValueAboveThreshold() {
        AlertRule rule = ruleWith("gt", 25.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        when(alertRuleRepository.save(any())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate).send(eq("alert.events"), any(), any());
    }

    @Test
    void gt_doesNotFireWhenValueBelowThreshold() {
        AlertRule rule = ruleWith("gt", 35.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void gte_firesWhenValueEqualsThreshold() {
        AlertRule rule = ruleWith("gte", 30.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        when(alertRuleRepository.save(any())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void lt_firesWhenValueBelowThreshold() {
        AlertRule rule = ruleWith("lt", 35.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        when(alertRuleRepository.save(any())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void lte_firesWhenValueEqualsThreshold() {
        AlertRule rule = ruleWith("lte", 30.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        when(alertRuleRepository.save(any())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void eq_firesOnExactMatch() {
        AlertRule rule = ruleWith("eq", 30.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        when(alertRuleRepository.save(any())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void eq_doesNotFireOnMismatch() {
        AlertRule rule = ruleWith("eq", 99.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void unknownCondition_doesNotFire() {
        AlertRule rule = ruleWith("between", 20.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void nullThreshold_doesNotFire() {
        AlertRule rule = ruleWith("gt", null, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    // ── suppression window ────────────────────────────────────────────────────

    @Test
    void suppressionWindow_preventsRepeatFiring() {
        AlertRule rule = ruleWith("gt", 25.0, 300);
        rule.setLastFiredAt(OffsetDateTime.now().minusSeconds(60)); // 60s ago, window is 300s
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void suppressionWindow_allowsFiringAfterWindowExpires() {
        AlertRule rule = ruleWith("gt", 25.0, 300);
        rule.setLastFiredAt(OffsetDateTime.now().minusSeconds(400)); // 400s ago, window is 300s
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        when(alertRuleRepository.save(any())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void suppressionWindow_nullLastFiredAt_alwaysAllows() {
        AlertRule rule = ruleWith("gt", 25.0, 300);
        // lastFiredAt is null by default
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        when(alertRuleRepository.save(any())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        verify(kafkaTemplate).send(any(), any(), any());
    }

    // ── non-numeric / null metrics ────────────────────────────────────────────

    @Test
    void nonNumericMetricValue_isSkipped() {
        ProcessedMessage stringMsg = new ProcessedMessage(projectId, "sensor_01",
                OffsetDateTime.now(), OffsetDateTime.now(),
                Map.of("status", "online"), Map.of());

        alertEvaluator.evaluate(stringMsg);

        verify(alertRuleRepository, never()).findActiveRulesForMetric(any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void nullMetrics_returnsImmediately() {
        ProcessedMessage nullMetricsMsg = new ProcessedMessage(projectId, "sensor_01",
                OffsetDateTime.now(), OffsetDateTime.now(), null, Map.of());

        alertEvaluator.evaluate(nullMetricsMsg);

        verify(alertRuleRepository, never()).findActiveRulesForMetric(any(), any(), any());
    }

    // ── alert fires lastFiredAt ───────────────────────────────────────────────

    @Test
    void fireAlert_stampsLastFiredAt() {
        AlertRule rule = ruleWith("gt", 25.0, null);
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("temperature"), any()))
                .thenReturn(List.of(rule));
        when(alertRuleRepository.findActiveRulesForMetric(any(), eq("humidity"), any()))
                .thenReturn(List.of());
        ArgumentCaptor<AlertRule> saved = ArgumentCaptor.forClass(AlertRule.class);
        when(alertRuleRepository.save(saved.capture())).thenReturn(rule);

        alertEvaluator.evaluate(msg);

        assertThat(saved.getValue().getLastFiredAt()).isNotNull();
        assertThat(saved.getValue().getLastFiredAt()).isAfter(OffsetDateTime.now().minusSeconds(5));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AlertRule ruleWith(String condition, Double threshold, Integer suppressionWindowS) {
        return AlertRule.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name("test rule")
                .metricName("temperature")
                .condition(condition)
                .threshold(threshold)
                .suppressionWindowSeconds(suppressionWindowS)
                .notificationChannels(new String[]{"email"})
                .build();
    }
}
