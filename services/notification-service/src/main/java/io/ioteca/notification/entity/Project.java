package io.ioteca.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor
public class Project {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "alert_email")
    private String alertEmail;
    @Column(name = "alert_webhook_url")
    private String alertWebhookUrl;
}
