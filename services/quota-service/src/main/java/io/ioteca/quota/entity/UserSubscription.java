package io.ioteca.quota.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_subscriptions")
@Getter @Setter @NoArgsConstructor
public class UserSubscription {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tier_id", nullable = false)
    private SubscriptionTier tier;

    @Column(nullable = false)
    private String status;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
