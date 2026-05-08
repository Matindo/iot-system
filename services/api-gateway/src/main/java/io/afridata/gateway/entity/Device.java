package io.afridata.gateway.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    private String name;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "hardware_type")
    private String hardwareType;

    @Column(name = "location_label")
    private String locationLabel;

    private Double latitude;
    private Double longitude;

    @Column(columnDefinition = "jsonb")
    private String tags;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "first_seen_at")
    private OffsetDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
