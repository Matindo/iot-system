package io.ioteca.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        String environment,
        String region,
        String timezone,
        Integer expectedDeviceCount,
        Integer declaredSendIntervalS,
        String dataFormat,
        Integer retentionOverrideDays,
        String alertWebhookUrl,
        String alertEmail
) {}
