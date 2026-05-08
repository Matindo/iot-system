package io.ioteca.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApiKeyCreateRequest(
        @NotNull UUID projectId,
        @NotBlank String name,
        String environment,
        List<String> scopes,
        OffsetDateTime expiresAt
) {
    public ApiKeyCreateRequest {
        environment = (environment == null) ? "live" : environment;
        scopes = (scopes == null || scopes.isEmpty()) ? List.of("ingest") : scopes;
    }
}
