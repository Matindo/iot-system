package io.ioteca.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.ingestion.dto.ProcessedMessage;
import io.ioteca.ingestion.dto.RawMessage;
import io.ioteca.ingestion.entity.Device;
import io.ioteca.ingestion.entity.SensorData;
import io.ioteca.ingestion.repository.DeviceRepository;
import io.ioteca.ingestion.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final SensorDataRepository sensorDataRepository;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_PAYLOAD_SIZE = 64 * 1024; // 64 KB

    @Transactional
    public ProcessedMessage ingest(UUID projectId, RawMessage msg) {
        if (msg.deviceId() == null || msg.deviceId().isBlank()) {
            throw new IllegalArgumentException("device_id is required");
        }
        if (msg.metrics() == null || msg.metrics().isEmpty()) {
            throw new IllegalArgumentException("metrics map must not be empty");
        }

        OffsetDateTime deviceTime = resolveTimestamp(msg.timestamp());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        String tagsJson = serializeToJson(msg.tags());

        // Explode the metrics map into one row per metric (narrow schema)
        List<SensorData> rows = new ArrayList<>(msg.metrics().size());
        for (Map.Entry<String, Object> entry : msg.metrics().entrySet()) {
            String metricName = entry.getKey();
            Object rawValue = entry.getValue();

            SensorData sd = SensorData.builder()
                    .time(deviceTime)
                    .projectId(projectId)
                    .deviceId(msg.deviceId())
                    .metric(metricName)
                    .ingestedAt(now)
                    .tags(tagsJson)
                    .build();

            if (rawValue instanceof Number n) {
                sd.setValue(n.doubleValue());
            } else {
                sd.setValueStr(String.valueOf(rawValue));
            }

            rows.add(sd);
        }

        // Bulk insert â€” each row goes via the native upsert
        for (SensorData row : rows) {
            sensorDataRepository.insertRaw(row);
        }

        // Auto-register device on first sight; update last_seen every time
        ensureDeviceExists(projectId, msg.deviceId(), now);

        return new ProcessedMessage(projectId, msg.deviceId(), deviceTime, now,
                msg.metrics(), msg.tags());
    }

    private void ensureDeviceExists(UUID projectId, String deviceId, OffsetDateTime now) {
        int updated = deviceRepository.touchLastSeen(projectId, deviceId, now);
        if (updated == 0) {
            // First time we see this device â€” auto-register it
            Device device = Device.builder()
                    .projectId(projectId)
                    .deviceId(deviceId)
                    .firstSeenAt(now)
                    .lastSeenAt(now)
                    .build();
            try {
                deviceRepository.save(device);
            } catch (Exception e) {
                // Race condition â€” another thread inserted first; that's fine
                log.debug("Device auto-register race: project={} device={}", projectId, deviceId);
            }
        }
    }

    private OffsetDateTime resolveTimestamp(Long epochMs) {
        if (epochMs == null) return OffsetDateTime.now(ZoneOffset.UTC);
        return Instant.ofEpochMilli(epochMs).atOffset(ZoneOffset.UTC);
    }

    private String serializeToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
