package io.ioteca.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id
    private UUID id;
    private String email;
    @Column(name = "full_name")
    private String fullName;
}
