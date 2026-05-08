package io.ioteca.auth.dto;

import io.ioteca.auth.entity.ApiKey;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Returned only at creation â€” contains the full plaintext key shown once.
 */
public record ApiKeyCreatedResponse(
        UUID id,
        UUID projectId,
        String name,
        String keyPrefix,
        String fullKey,
        String environment,
        String[] scopes,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {
    public static ApiKeyCreatedResponse from(ApiKey k, String fullKey) {
        return new ApiKeyCreatedResponse(k.getId(), k.getProjectId(), k.getName(),
                k.getKeyPrefix(), fullKey, k.getEnvironment(), k.getScopes(),
                k.getCreatedAt(), k.getExpiresAt());
    }
}
