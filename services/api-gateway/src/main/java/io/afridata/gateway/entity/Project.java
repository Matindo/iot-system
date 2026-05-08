package io.afridata.gateway.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private String environment = "production";

    @Column(nullable = false)
    @Builder.Default
    private String region = "af-ke-1";

    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Africa/Nairobi";

    @Column(name = "expected_device_count")
    @Builder.Default
    private int expectedDeviceCount = 1;

    @Column(name = "declared_send_interval_s")
    private Integer declaredSendIntervalS;

    @Column(name = "data_format")
    @Builder.Default
    private String dataFormat = "json";

    @Column(name = "retention_override_days")
    private Integer retentionOverrideDays;

    @Column(name = "alert_webhook_url")
    private String alertWebhookUrl;

    @Column(name = "alert_email")
    private String alertEmail;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
