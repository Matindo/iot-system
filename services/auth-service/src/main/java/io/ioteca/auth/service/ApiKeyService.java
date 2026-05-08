package io.ioteca.auth.service;

import io.ioteca.auth.dto.ApiKeyCreateRequest;
import io.ioteca.auth.dto.ApiKeyCreatedResponse;
import io.ioteca.auth.dto.ApiKeyResponse;
import io.ioteca.auth.entity.ApiKey;
import io.ioteca.auth.repository.ApiKeyRepository;
import io.ioteca.auth.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String PREFIX = "ioteca";
    private static final int SECRET_BYTES = 15; // produces 20-char URL-safe base64

    private final ApiKeyRepository apiKeyRepository;
    private final ProjectRepository projectRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiKeyCreatedResponse createKey(UUID userId, ApiKeyCreateRequest req) {
        projectRepository.findByIdAndUserId(req.projectId(), userId)
                .filter(p -> p.isActive())
                .orElseThrow(() -> new AccessDeniedException("Project not found or not owned by user"));

        String projectShortId = req.projectId().toString().replace("-", "").substring(0, 8);
        String env = req.environment();

        byte[] secretBytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secretBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        String fullKey = String.format("%s_%s_%s_%s", PREFIX, env, projectShortId, secret);
        String keyHash = sha256Hex(fullKey);
        String keyPrefix = fullKey.substring(0, Math.min(20, fullKey.length()));

        ApiKey apiKey = ApiKey.builder()
                .projectId(req.projectId())
                .name(req.name())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .environment(env)
                .scopes(req.scopes().toArray(new String[0]))
                .expiresAt(req.expiresAt())
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        return ApiKeyCreatedResponse.from(apiKey, fullKey);
    }

    public List<ApiKeyResponse> listKeys(UUID userId, UUID projectId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException("Project not found or not owned by user");
        }
        return apiKeyRepository.findByProjectId(projectId)
                .stream().map(ApiKeyResponse::from).toList();
    }

    @Transactional
    public void revokeKey(UUID userId, UUID keyId, String reason) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        projectRepository.findByIdAndUserId(key.getProjectId(), userId)
                .orElseThrow(() -> new AccessDeniedException("Not authorized to revoke this key"));

        key.setActive(false);
        key.setRevokedAt(OffsetDateTime.now());
        key.setRevokeReason(reason);
        apiKeyRepository.save(key);
    }

    public Optional<ApiKey> validateKey(String rawKey) {
        String hash = sha256Hex(rawKey);
        return apiKeyRepository.findByKeyHash(hash)
                .filter(ApiKey::isActive)
                .filter(k -> k.getExpiresAt() == null || k.getExpiresAt().isAfter(OffsetDateTime.now()));
    }

    @Transactional
    public void touchLastUsed(UUID keyId) {
        apiKeyRepository.findById(keyId).ifPresent(k -> {
            k.setLastUsedAt(OffsetDateTime.now());
            apiKeyRepository.save(k);
        });
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
