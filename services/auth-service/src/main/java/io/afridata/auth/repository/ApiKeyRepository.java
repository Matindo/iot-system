package io.afridata.auth.repository;

import io.afridata.auth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByProjectIdAndIsActiveTrue(UUID projectId);
    List<ApiKey> findByProjectId(UUID projectId);
}
