package io.ioteca.auth.service;

import io.ioteca.auth.dto.ApiKeyCreateRequest;
import io.ioteca.auth.dto.ApiKeyCreatedResponse;
import io.ioteca.auth.entity.ApiKey;
import io.ioteca.auth.entity.Project;
import io.ioteca.auth.repository.ApiKeyRepository;
import io.ioteca.auth.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock ApiKeyRepository apiKeyRepository;
    @Mock ProjectRepository projectRepository;
    @InjectMocks ApiKeyService apiKeyService;

    private UUID userId;
    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        projectId = UUID.randomUUID();
        project   = Project.builder().id(projectId).userId(userId).isActive(true).build();
    }

    // ── createKey ────────────────────────────────────────────────────────────

    @Test
    void createKey_returnsKeyWithCorrectFormat() {
        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        ApiKey saved = ApiKey.builder().id(UUID.randomUUID()).projectId(projectId)
                .keyPrefix("ioteca_live_").keyHash("hash").environment("live")
                .scopes(new String[]{"ingest"}).build();
        when(apiKeyRepository.save(any())).thenReturn(saved);

        ApiKeyCreatedResponse resp = apiKeyService.createKey(userId,
                new ApiKeyCreateRequest(projectId, "My Key", "live", List.of("ingest"), null));

        assertThat(resp.fullKey()).matches("ioteca_live_[a-z0-9]{8}_[A-Za-z0-9_-]+");
    }

    @Test
    void createKey_keyPrefixIsFirst20Chars() {
        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(apiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyCreatedResponse resp = apiKeyService.createKey(userId,
                new ApiKeyCreateRequest(projectId, "My Key", "live", List.of("ingest"), null));

        assertThat(resp.fullKey()).startsWith(resp.keyPrefix());
        assertThat(resp.keyPrefix()).hasSizeLessThanOrEqualTo(20);
    }

    @Test
    void createKey_throwsWhenProjectNotOwned() {
        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.createKey(userId,
                new ApiKeyCreateRequest(projectId, "Key", "live", List.of("ingest"), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createKey_throwsWhenProjectInactive() {
        project.setActive(false);
        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> apiKeyService.createKey(userId,
                new ApiKeyCreateRequest(projectId, "Key", "live", List.of("ingest"), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── validateKey ──────────────────────────────────────────────────────────

    @Test
    void validateKey_emptyForUnknownHash() {
        when(apiKeyRepository.findByKeyHash(any())).thenReturn(Optional.empty());
        assertThat(apiKeyService.validateKey("ioteca_live_deadbeef_unknownkey")).isEmpty();
    }

    @Test
    void validateKey_emptyForRevokedKey() {
        ApiKey revoked = ApiKey.builder().id(UUID.randomUUID()).projectId(projectId)
                .keyHash("h").isActive(false).build();
        when(apiKeyRepository.findByKeyHash(any())).thenReturn(Optional.of(revoked));

        assertThat(apiKeyService.validateKey("somekey")).isEmpty();
    }

    @Test
    void validateKey_emptyForExpiredKey() {
        ApiKey expired = ApiKey.builder().id(UUID.randomUUID()).projectId(projectId)
                .keyHash("h").isActive(true)
                .expiresAt(OffsetDateTime.now().minusHours(1)).build();
        when(apiKeyRepository.findByKeyHash(any())).thenReturn(Optional.of(expired));

        assertThat(apiKeyService.validateKey("somekey")).isEmpty();
    }

    @Test
    void validateKey_presentForActiveNonExpired() {
        ApiKey active = ApiKey.builder().id(UUID.randomUUID()).projectId(projectId)
                .keyHash("h").isActive(true).build();
        when(apiKeyRepository.findByKeyHash(any())).thenReturn(Optional.of(active));

        assertThat(apiKeyService.validateKey("somekey")).isPresent();
    }

    // ── revokeKey ────────────────────────────────────────────────────────────

    @Test
    void revokeKey_setsActiveFalseAndStampsTime() {
        UUID keyId = UUID.randomUUID();
        ApiKey key = ApiKey.builder().id(keyId).projectId(projectId).isActive(true).build();
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(key));
        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(apiKeyRepository.save(any())).thenReturn(key);

        apiKeyService.revokeKey(userId, keyId, "no longer needed");

        assertThat(key.isActive()).isFalse();
        assertThat(key.getRevokedAt()).isNotNull();
        assertThat(key.getRevokeReason()).isEqualTo("no longer needed");
    }

    @Test
    void revokeKey_throwsWhenKeyNotFound() {
        UUID keyId = UUID.randomUUID();
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> apiKeyService.revokeKey(userId, keyId, "reason"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revokeKey_throwsWhenProjectNotOwned() {
        UUID keyId = UUID.randomUUID();
        ApiKey key = ApiKey.builder().id(keyId).projectId(projectId).build();
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(key));
        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> apiKeyService.revokeKey(userId, keyId, "reason"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
