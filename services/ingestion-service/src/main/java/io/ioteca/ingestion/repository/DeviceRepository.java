package io.ioteca.ingestion.repository;

import io.ioteca.ingestion.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByProjectIdAndDeviceId(UUID projectId, String deviceId);

    @Modifying
    @Query("""
        UPDATE Device d SET d.lastSeenAt = :now, d.updatedAt = :now
        WHERE d.projectId = :projectId AND d.deviceId = :deviceId
        """)
    int touchLastSeen(UUID projectId, String deviceId, OffsetDateTime now);
}
