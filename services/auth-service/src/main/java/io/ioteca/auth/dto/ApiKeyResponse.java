package io.ioteca.auth.dto;

import io.ioteca.auth.entity.ApiKey;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        UUID projectId,
        String name,
        String keyPrefix,
        String environment,
        String[] scopes,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime lastUsedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt
) {
    public static ApiKeyResponse from(ApiKey k) {
        return new ApiKeyResponse(k.getId(), k.getProjectId(), k.getName(),
                k.getKeyPrefix(), k.getEnvironment(), k.getScopes(),
                k.isActive(), k.getCreatedAt(), k.getLastUsedAt(),
                k.getExpiresAt(), k.getRevokedAt());
    }
}
