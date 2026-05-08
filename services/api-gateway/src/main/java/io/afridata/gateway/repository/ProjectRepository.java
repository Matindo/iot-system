package io.afridata.gateway.repository;

import io.afridata.gateway.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<Project> findByIdAndUserId(UUID id, UUID userId);
    long countByUserIdAndIsActiveTrue(UUID userId);
}
