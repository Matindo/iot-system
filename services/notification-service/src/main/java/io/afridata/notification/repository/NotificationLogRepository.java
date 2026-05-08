package io.afridata.notification.repository;

import io.afridata.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByProjectIdAndTypeAndSentAtAfter(UUID projectId, String type, OffsetDateTime after);
}
