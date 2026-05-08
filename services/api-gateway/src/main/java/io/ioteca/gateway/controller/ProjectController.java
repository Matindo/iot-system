package io.ioteca.gateway.controller;

import io.ioteca.gateway.dto.ProjectRequest;
import io.ioteca.gateway.entity.Project;
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
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final JwtService jwtService;

    @GetMapping
    public List<Project> listProjects(@RequestHeader("Authorization") String auth) {
        UUID userId = extractUserId(auth);
        return projectRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @PostMapping
    public ResponseEntity<Project> createProject(
            @RequestHeader("Authorization") String auth,
            @Valid @RequestBody ProjectRequest req) {
        UUID userId = extractUserId(auth);

        Project project = Project.builder()
                .userId(userId)
                .name(req.name())
                .description(req.description())
                .environment(req.environment() != null ? req.environment() : "production")
                .region(req.region() != null ? req.region() : "af-ke-1")
                .timezone(req.timezone() != null ? req.timezone() : "Africa/Nairobi")
                .expectedDeviceCount(req.expectedDeviceCount() != null ? req.expectedDeviceCount() : 1)
                .declaredSendIntervalS(req.declaredSendIntervalS())
                .dataFormat(req.dataFormat() != null ? req.dataFormat() : "json")
                .retentionOverrideDays(req.retentionOverrideDays())
                .alertWebhookUrl(req.alertWebhookUrl())
                .alertEmail(req.alertEmail())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(projectRepository.save(project));
    }

    @GetMapping("/{id}")
    public Project getProject(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID id) {
        UUID userId = extractUserId(auth);
        return projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("Project not found"));
    }

    @PutMapping("/{id}")
    public Project updateProject(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest req) {
        UUID userId = extractUserId(auth);
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("Project not found"));

        if (req.name() != null)        project.setName(req.name());
        if (req.description() != null) project.setDescription(req.description());
        if (req.alertWebhookUrl() != null) project.setAlertWebhookUrl(req.alertWebhookUrl());
        if (req.alertEmail() != null)  project.setAlertEmail(req.alertEmail());
        project.setUpdatedAt(OffsetDateTime.now());

        return projectRepository.save(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID id) {
        UUID userId = extractUserId(auth);
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("Project not found"));
        project.setActive(false);
        project.setUpdatedAt(OffsetDateTime.now());
        projectRepository.save(project);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(String auth) {
        return jwtService.extractUserId(auth.substring(7));
    }
}
