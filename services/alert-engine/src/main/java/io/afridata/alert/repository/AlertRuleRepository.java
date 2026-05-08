package io.afridata.alert.repository;

import io.afridata.alert.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    @Query("""
        SELECT r FROM AlertRule r
        WHERE r.projectId = :projectId
          AND r.isActive = true
          AND r.metricName = :metric
          AND (r.deviceId IS NULL OR r.deviceId = :deviceId)
        """)
    List<AlertRule> findActiveRulesForMetric(UUID projectId, String metric, String deviceId);

    List<AlertRule> findByProjectIdAndIsActiveTrue(UUID projectId);
}
