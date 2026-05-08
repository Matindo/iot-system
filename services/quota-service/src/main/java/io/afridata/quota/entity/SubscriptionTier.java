package io.afridata.quota.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscription_tiers")
@Getter @Setter @NoArgsConstructor
public class SubscriptionTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(name = "max_messages_per_day")
    private long maxMessagesPerDay;

    @Column(name = "max_message_rate_per_sec")
    private int maxMessageRatePerSec;

    @Column(name = "storage_limit_mb")
    private long storageLimitMb;
}
