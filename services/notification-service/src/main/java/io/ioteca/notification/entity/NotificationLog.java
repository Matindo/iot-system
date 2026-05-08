package io.ioteca.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    /** QUOTA_WARNING | QUOTA_EXCEEDED | ALERT | SYSTEM */
    @Column(nullable = false)
    private String type;

    /** EMAIL | SMS | WEBHOOK */
    @Column(nullable = false)
    private String channel;

    private String subject;
    private String body;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private OffsetDateTime sentAt = OffsetDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean delivered = false;

    private String error;
}
