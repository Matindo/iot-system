package io.ioteca.gateway.repository;

import io.ioteca.gateway.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
    List<AlertRule> findByProjectIdAndIsActiveTrue(UUID projectId);
    Optional<AlertRule> findByIdAndProjectId(UUID id, UUID projectId);
}
