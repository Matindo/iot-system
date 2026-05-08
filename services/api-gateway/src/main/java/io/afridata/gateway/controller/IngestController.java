package io.afridata.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.afridata.gateway.dto.IngestRequest;
import io.afridata.gateway.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
@Slf4j
public class IngestController {

    private static final String RAW_TOPIC = "raw.ingest.af-ke-1";
    private static final int MAX_BATCH = 100;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> ingest(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody IngestRequest req) {
        UUID projectId = resolveProjectFromKey(authHeader);
        publishToKafka(projectId, req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody List<IngestRequest> requests) {

        if (requests.size() > MAX_BATCH) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Batch size exceeds maximum of " + MAX_BATCH));
        }

        UUID projectId = resolveProjectFromKey(authHeader);
        int accepted = 0;
        for (IngestRequest req : requests) {
            try {
                publishToKafka(projectId, req);
                accepted++;
            } catch (Exception e) {
                log.warn("Failed to publish message for device {}: {}", req.deviceId(), e.getMessage());
            }
        }

        return ResponseEntity.accepted().body(Map.of(
                "accepted", accepted,
                "total", requests.size()
        ));
    }

    private void publishToKafka(UUID projectId, IngestRequest req) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("device_id", req.deviceId());
            payload.put("timestamp", req.timestamp());
            payload.put("metrics", req.metrics());
            payload.put("tags", req.tags());
            payload.put("project_id", projectId.toString());

            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(RAW_TOPIC, projectId.toString(), json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish ingest message", e);
        }
    }

    private UUID resolveProjectFromKey(String authHeader) {
        // Bearer token is always a JWT at this point (filter already validated it)
        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }
}
