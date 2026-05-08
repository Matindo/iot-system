package io.afridata.quota.repository;

import io.afridata.quota.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    @Query("""
        SELECT s FROM UserSubscription s
        JOIN Project p ON p.id = :projectId
        WHERE s.userId = p.userId AND s.status = 'ACTIVE'
        ORDER BY s.expiresAt DESC
        """)
    Optional<UserSubscription> findActiveForProject(UUID projectId);
}
