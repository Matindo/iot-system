package io.ioteca.gateway.controller;

import io.ioteca.gateway.entity.Device;
import io.ioteca.gateway.entity.Project;
import io.ioteca.gateway.repository.DeviceRepository;
import io.ioteca.gateway.repository.ProjectRepository;
import io.ioteca.gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final ProjectRepository projectRepository;
    private final JwtService jwtService;

    @GetMapping
    public List<Device> listDevices(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId) {
        assertProjectOwnership(auth, projectId);
        return deviceRepository.findByProjectIdAndIsActiveTrue(projectId);
    }

    @GetMapping("/{deviceId}")
    public Device getDevice(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId,
            @PathVariable String deviceId) {
        assertProjectOwnership(auth, projectId);
        return deviceRepository.findByProjectIdAndDeviceId(projectId, deviceId)
                .orElseThrow(() -> new AccessDeniedException("Device not found"));
    }

    @PatchMapping("/{deviceId}")
    public Device updateDevice(
            @RequestHeader("Authorization") String auth,
            @PathVariable UUID projectId,
            @PathVariable String deviceId,
            @RequestBody Device patch) {
        assertProjectOwnership(auth, projectId);
        Device device = deviceRepository.findByProjectIdAndDeviceId(projectId, deviceId)
                .orElseThrow(() -> new AccessDeniedException("Device not found"));

        if (patch.getName() != null)          device.setName(patch.getName());
        if (patch.getLocationLabel() != null) device.setLocationLabel(patch.getLocationLabel());
        if (patch.getLatitude() != null)      device.setLatitude(patch.getLatitude());
        if (patch.getLongitude() != null)     device.setLongitude(patch.getLongitude());
        if (patch.getTags() != null)          device.setTags(patch.getTags());

        return deviceRepository.save(device);
    }

    private void assertProjectOwnership(String auth, UUID projectId) {
        UUID userId = jwtService.extractUserId(auth.substring(7));
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("Project not found"));
    }
}
