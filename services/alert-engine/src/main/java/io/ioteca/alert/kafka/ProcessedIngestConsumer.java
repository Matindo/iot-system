package io.ioteca.alert.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.alert.dto.ProcessedMessage;
import io.ioteca.alert.service.AlertEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedIngestConsumer {

    private final AlertEvaluator alertEvaluator;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "processed.ingest.af-ke-1",
            groupId = "alert-engine-group",
            concurrency = "2"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ProcessedMessage msg = objectMapper.readValue(record.value(), ProcessedMessage.class);
            alertEvaluator.evaluate(msg);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Alert evaluation failed for key={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge(); // don't block pipeline on evaluation errors
        }
    }
}
