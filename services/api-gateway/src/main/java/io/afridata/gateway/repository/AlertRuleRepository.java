package io.afridata.gateway.repository;

import io.afridata.gateway.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
    List<AlertRule> findByProjectIdAndIsActiveTrue(UUID projectId);
    Optional<AlertRule> findByIdAndProjectId(UUID id, UUID projectId);
}
