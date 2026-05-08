package io.ioteca.auth.repository;

import io.ioteca.auth.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    Optional<UserSubscription> findTopByUserIdAndStatusOrderByStartedAtDesc(UUID userId, String status);
}
