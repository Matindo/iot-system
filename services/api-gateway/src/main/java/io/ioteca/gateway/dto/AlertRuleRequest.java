package io.ioteca.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertRuleRequest(
        @NotBlank String name,
        String description,
        @NotBlank String metricName,
        String deviceId,
        @NotNull String condition,
        Double threshold,
        Integer absenceWindowS,
        String[] notificationChannels,
        Integer suppressionWindowS
) {}
