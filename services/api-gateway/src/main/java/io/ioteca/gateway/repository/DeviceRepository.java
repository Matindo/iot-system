package io.ioteca.gateway.repository;

import io.ioteca.gateway.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByProjectIdAndIsActiveTrue(UUID projectId);
    Optional<Device> findByProjectIdAndDeviceId(UUID projectId, String deviceId);
}
