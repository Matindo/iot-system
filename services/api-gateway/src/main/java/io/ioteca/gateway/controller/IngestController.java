package io.ioteca.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.gateway.dto.IngestRequest;
import io.ioteca.gateway.filter.ApiKeyAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final int MAX_BATCH    = 100;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Single reading ingest.
     *
     * Auth options:
     *  - API key (device path): project_id resolved automatically from the key.
     *  - JWT   (developer/dashboard path): pass ?projectId={uuid} query param.
     */
    @PostMapping
    public ResponseEntity<Void> ingest(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID projectId,
            @Valid @RequestBody IngestRequest req) {
        UUID resolved = resolveProjectId(httpRequest, projectId);
        publish(resolved, req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID projectId,
            @Valid @RequestBody List<IngestRequest> requests) {

        if (requests.size() > MAX_BATCH) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Batch size exceeds maximum of " + MAX_BATCH));
        }

        UUID resolved = resolveProjectId(httpRequest, projectId);
        int accepted  = 0;
        for (IngestRequest req : requests) {
            try {
                publish(resolved, req);
                accepted++;
            } catch (Exception e) {
                log.warn("Skipped message for device {}: {}", req.deviceId(), e.getMessage());
            }
        }

        return ResponseEntity.accepted().body(Map.of(
                "accepted", accepted,
                "total",    requests.size()
        ));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID resolveProjectId(HttpServletRequest request, UUID queryParam) {
        // API key path — project_id set by ApiKeyAuthFilter
        UUID fromKey = (UUID) request.getAttribute(ApiKeyAuthFilter.PROJECT_ID_ATTR);
        if (fromKey != null) return fromKey;

        // JWT path — caller must supply projectId as a query parameter
        if (queryParam != null) return queryParam;

        throw new IllegalArgumentException(
                "projectId query parameter is required when authenticating with a JWT");
    }

    private void publish(UUID projectId, IngestRequest req) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("device_id",  req.deviceId());
            payload.put("timestamp",  req.timestamp());
            payload.put("metrics",    req.metrics());
            payload.put("tags",       req.tags());
            payload.put("project_id", projectId.toString());

            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(RAW_TOPIC, projectId.toString(), json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish ingest message", e);
        }
    }
}
