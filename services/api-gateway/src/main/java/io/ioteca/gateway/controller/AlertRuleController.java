package io.ioteca.gateway.controller;

import io.ioteca.gateway.dto.AlertRuleRequest;
import io.ioteca.gateway.entity.AlertRule;
import io.ioteca.gateway.repository.AlertRuleRepository;
import io.ioteca.gateway.repository.ProjectRepository;
import io.ioteca.gateway.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;
    private final ProjectRepository projectRepository;
    private final JwtService jwtService;

    @GetMapping
    public List<AlertRule> list(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId) {
        verifyOwnership(auth, projectId);
        return alertRuleRepository.findByProjectIdAndIsActiveTrue(projectId);
    }

    @PostMapping
    public ResponseEntity<AlertRule> create(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId,
            @Valid @RequestBody AlertRuleRequest req) {
        verifyOwnership(auth, projectId);

        AlertRule rule = AlertRule.builder()
                .projectId(projectId)
                .name(req.name())
                .description(req.description())
                .metricName(req.metricName())
                .deviceId(req.deviceId())
                .condition(req.condition())
                .threshold(req.threshold())
                .absenceWindowS(req.absenceWindowS())
                .notificationChannels(req.notificationChannels() != null
                        ? req.notificationChannels() : new String[]{"email"})
                .suppressionWindowS(req.suppressionWindowS() != null
                        ? req.suppressionWindowS() : 300)
                .createdAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alertRuleRepository.save(rule));
    }

    @PutMapping("/{id}")
    public AlertRule update(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody AlertRuleRequest req) {
        verifyOwnership(auth, projectId);

        AlertRule rule = alertRuleRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new AccessDeniedException("Alert rule not found"));

        if (req.name() != null)                    rule.setName(req.name());
        if (req.description() != null)             rule.setDescription(req.description());
        if (req.metricName() != null)              rule.setMetricName(req.metricName());
        if (req.condition() != null)               rule.setCondition(req.condition());
        if (req.threshold() != null)               rule.setThreshold(req.threshold());
        if (req.suppressionWindowS() != null)      rule.setSuppressionWindowS(req.suppressionWindowS());
        if (req.notificationChannels() != null)    rule.setNotificationChannels(req.notificationChannels());

        return alertRuleRepository.save(rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        verifyOwnership(auth, projectId);

        AlertRule rule = alertRuleRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new AccessDeniedException("Alert rule not found"));
        rule.setActive(false);
        alertRuleRepository.save(rule);
        return ResponseEntity.noContent().build();
    }

    private void verifyOwnership(String auth, UUID projectId) {
        UUID userId = jwtService.extractUserId(auth.substring(7));
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("Project not found"));
    }
}
