package io.ioteca.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record IngestRequest(
        @NotBlank @JsonProperty("device_id") String deviceId,
        Long timestamp,
        @NotEmpty Map<String, Object> metrics,
        Map<String, Object> tags
) {}
