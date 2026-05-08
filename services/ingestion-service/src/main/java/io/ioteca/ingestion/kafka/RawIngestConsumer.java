package io.ioteca.ingestion.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.ingestion.dto.ProcessedMessage;
import io.ioteca.ingestion.dto.RawMessage;
import io.ioteca.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawIngestConsumer {

    private static final String PROCESSED_TOPIC = "processed.ingest.af-ke-1";

    private final IngestionService ingestionService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(
            topics = "raw.ingest.af-ke-1",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            RawMessage msg = objectMapper.readValue(record.value(), RawMessage.class);

            // project_id is embedded in the Kafka message key (set by api-gateway / EMQX bridge)
            UUID projectId = resolveProjectId(record, msg);
            if (projectId == null) {
                log.warn("Dropping message â€” no project_id resolvable. key={}", record.key());
                ack.acknowledge();
                return;
            }

            ProcessedMessage processed = ingestionService.ingest(projectId, msg);

            String processedJson = objectMapper.writeValueAsString(processed);
            kafkaTemplate.send(PROCESSED_TOPIC, record.key(), processedJson);

            ack.acknowledge();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid message dropped: {} â€” {}", record.key(), e.getMessage());
            ack.acknowledge(); // don't retry bad messages
        } catch (Exception e) {
            log.error("Failed to ingest message key={}: {}", record.key(), e.getMessage(), e);
            // Do NOT ack â€” let Kafka retry based on max.poll.interval
        }
    }

    private UUID resolveProjectId(ConsumerRecord<?, ?> record, RawMessage msg) {
        // Prefer project_id from the message body (set by api-gateway on HTTP ingest)
        if (msg.projectId() != null) return msg.projectId();
        // Fall back to the Kafka message key (set by EMQX bridge from topic name)
        try {
            return UUID.fromString(String.valueOf(record.key()));
        } catch (Exception e) {
            return null;
        }
    }
}
