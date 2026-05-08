package io.afridata.gateway.controller;

import io.afridata.gateway.repository.AlertRuleRepository;
import io.afridata.gateway.repository.DeviceRepository;
import io.afridata.gateway.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectRepository projectRepository;
    private final DeviceRepository deviceRepository;
    private final AlertRuleRepository alertRuleRepository;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Long totalUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM platform.users WHERE is_active = true", Long.class);
        long totalProjects = projectRepository.count();
        long totalDevices = deviceRepository.count();
        long totalAlertRules = alertRuleRepository.count();

        Long totalMessages = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(messages_today), 0) FROM tsdata.project_quota_snapshots"
                + " WHERE time >= NOW() - INTERVAL '25 hours'",
                Long.class);

        return Map.of(
                "totalUsers",       totalUsers != null ? totalUsers : 0L,
                "totalProjects",    totalProjects,
                "totalDevices",     totalDevices,
                "totalAlertRules",  totalAlertRules,
                "messagesLast24h",  totalMessages != null ? totalMessages : 0L
        );
    }
}
