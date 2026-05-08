package io.afridata.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Enriched message emitted to processed.ingest.* for downstream consumers
 * (alert-engine, quota-service).
 */
public record ProcessedMessage(
        UUID projectId,
        String deviceId,
        OffsetDateTime time,
        OffsetDateTime ingestedAt,
        Map<String, Object> metrics,
        Map<String, Object> tags
) {}
