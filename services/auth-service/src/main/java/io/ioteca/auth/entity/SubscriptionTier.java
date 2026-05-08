package io.ioteca.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "subscription_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "max_messages_per_day", nullable = false)
    private long maxMessagesPerDay;

    @Column(name = "max_devices", nullable = false)
    private int maxDevices;

    @Column(name = "max_projects", nullable = false)
    private int maxProjects;

    @Column(name = "data_retention_days", nullable = false)
    private int dataRetentionDays;

    @Column(name = "max_message_rate_per_sec", nullable = false)
    private int maxMessageRatePerSec;

    @Column(name = "storage_limit_mb", nullable = false)
    private long storageLimitMb;

    @Column(name = "api_calls_per_minute", nullable = false)
    private int apiCallsPerMinute;

    @Column(name = "price_kes_monthly", nullable = false)
    private BigDecimal priceKesMonthly;

    // stored as JSONB â€” read as string to avoid extra dependency
    @Column(columnDefinition = "jsonb")
    private String features;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
