package io.ioteca.quota.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.quota.service.QuotaService;
import io.ioteca.quota.service.QuotaService.QuotaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawIngestConsumer {

    private static final String QUOTA_TOPIC = "quota.events";

    private final QuotaService quotaService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "raw.ingest.af-ke-1",
            groupId = "quota-service-group",
            concurrency = "2"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            UUID projectId = resolveProjectId(record);
            if (projectId == null) {
                ack.acknowledge();
                return;
            }

            QuotaStatus status = quotaService.increment(projectId);

            if (status != QuotaStatus.OK) {
                emitQuotaEvent(projectId, status);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Quota processing failed for key={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge();
        }
    }

    private void emitQuotaEvent(UUID projectId, QuotaStatus status) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "projectId", projectId.toString(),
                    "status", status.name(),
                    "count", quotaService.currentCount(projectId),
                    "limit", quotaService.dailyLimit(projectId)
            ));
            kafkaTemplate.send(QUOTA_TOPIC, projectId.toString(), payload);
        } catch (Exception e) {
            log.error("Failed to emit quota event for project {}: {}", projectId, e.getMessage());
        }
    }

    private UUID resolveProjectId(ConsumerRecord<?, ?> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(
                    record.value().toString(), Map.class);
            Object pid = msg.get("project_id");
            if (pid != null) return UUID.fromString(pid.toString());
        } catch (Exception ignored) {}
        try {
            return UUID.fromString(String.valueOf(record.key()));
        } catch (Exception e) {
            return null;
        }
    }
}
