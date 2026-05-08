package io.ioteca.gateway.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Read-only view of platform.api_keys used by the gateway to resolve project context. */
@Entity
@Table(name = "api_keys")
@Getter
public class ApiKey {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
