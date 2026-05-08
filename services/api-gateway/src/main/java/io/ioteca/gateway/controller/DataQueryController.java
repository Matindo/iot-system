package io.ioteca.gateway.controller;

import io.ioteca.gateway.repository.ProjectRepository;
import io.ioteca.gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/data")
@RequiredArgsConstructor
public class DataQueryController {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectRepository projectRepository;
    private final JwtService jwtService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> query(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId,
            @RequestParam String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String deviceId) {

        verifyOwnership(auth, projectId);

        // Set RLS session variable for the duration of this transaction
        jdbcTemplate.execute("SET LOCAL app.current_project_id = '" + projectId + "'");

        long hours = ChronoUnit.HOURS.between(from, to);
        String resolution;
        String sql;

        if (hours <= 24) {
            resolution = "raw";
            sql = rawSql(deviceId);
        } else if (hours <= 168) {
            resolution = "1min";
            sql = aggSql("tsdata.sensor_data_1min", deviceId);
        } else if (hours <= 720) {
            resolution = "1hr";
            sql = aggSql("tsdata.sensor_data_1hr", deviceId);
        } else {
            resolution = "1day";
            sql = aggSql("tsdata.sensor_data_1day", deviceId);
        }

        List<Object> params = new ArrayList<>(Arrays.asList(projectId, metric, from, to));
        if (deviceId != null) params.add(deviceId);

        List<Map<String, Object>> points = jdbcTemplate.queryForList(sql, params.toArray());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resolution", resolution);
        response.put("projectId", projectId);
        response.put("metric", metric);
        if (deviceId != null) response.put("deviceId", deviceId);
        response.put("from", from);
        response.put("to", to);
        response.put("count", points.size());
        response.put("points", points);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> listMetrics(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId,
            @RequestParam(required = false) String deviceId) {

        verifyOwnership(auth, projectId);
        jdbcTemplate.execute("SET LOCAL app.current_project_id = '" + projectId + "'");

        List<Object> params = new ArrayList<>();
        params.add(projectId);
        String deviceFilter = "";
        if (deviceId != null) {
            deviceFilter = " AND device_id = ?";
            params.add(deviceId);
        }

        String sql = """
            SELECT DISTINCT metric FROM tsdata.sensor_data
            WHERE project_id = ?%s
            ORDER BY metric ASC
            """.formatted(deviceFilter);

        List<String> metrics = jdbcTemplate.queryForList(sql, String.class, params.toArray());
        return ResponseEntity.ok(metrics);
    }

    private String rawSql(String deviceId) {
        String filter = deviceId != null ? " AND device_id = ?" : "";
        return """
            SELECT time, device_id, value AS avg_value, value AS min_value, value AS max_value, 1 AS sample_count
            FROM tsdata.sensor_data
            WHERE project_id = ? AND metric = ? AND time >= ? AND time <= ?%s
            ORDER BY time ASC
            LIMIT 5000
            """.formatted(filter);
    }

    private String aggSql(String view, String deviceId) {
        String filter = deviceId != null ? " AND device_id = ?" : "";
        return """
            SELECT bucket AS time, device_id, avg_value, min_value, max_value, sample_count
            FROM %s
            WHERE project_id = ? AND metric = ? AND bucket >= ? AND bucket <= ?%s
            ORDER BY bucket ASC
            """.formatted(view, filter);
    }

    private void verifyOwnership(String auth, UUID projectId) {
        UUID userId = jwtService.extractUserId(auth.substring(7));
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("Project not found"));
    }
}
