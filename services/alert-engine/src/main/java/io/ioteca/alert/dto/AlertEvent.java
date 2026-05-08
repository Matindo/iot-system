package io.ioteca.alert.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertEvent(
        UUID ruleId,
        UUID projectId,
        String deviceId,
        String metricName,
        double actualValue,
        String condition,
        double threshold,
        OffsetDateTime firedAt,
        String[] notificationChannels
) {}
