package io.afridata.alert.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "device_id")
    private String deviceId;

    /** gt | lt | eq | gte | lte | absence */
    @Column(nullable = false)
    private String condition;

    private Double threshold;

    @Column(name = "absence_window_s")
    private Integer absenceWindowSeconds;

    @Column(name = "notification_channels", columnDefinition = "text[]", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] notificationChannels;

    @Column(name = "suppression_window_s")
    @Builder.Default
    private Integer suppressionWindowSeconds = 300;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "last_fired_at")
    private OffsetDateTime lastFiredAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
