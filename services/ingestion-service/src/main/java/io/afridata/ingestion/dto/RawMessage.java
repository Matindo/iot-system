package io.afridata.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

/**
 * Canonical ingest payload — devices send this via MQTT or HTTP.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawMessage(
        @JsonProperty("device_id")  String deviceId,
        Long timestamp,
        Map<String, Object> metrics,
        Map<String, Object> tags,
        @JsonProperty("project_id") UUID projectId
) {}
