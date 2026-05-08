package io.ioteca.auth.repository;

import io.ioteca.auth.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
