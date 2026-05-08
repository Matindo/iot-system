package io.ioteca.alert.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ProcessedMessage(
        UUID projectId,
        String deviceId,
        OffsetDateTime time,
        OffsetDateTime ingestedAt,
        Map<String, Object> metrics,
        Map<String, Object> tags
) {}
