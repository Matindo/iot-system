package io.ioteca.auth.repository;

import io.ioteca.auth.entity.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionTierRepository extends JpaRepository<SubscriptionTier, Integer> {
    Optional<SubscriptionTier> findByNameAndIsActiveTrue(String name);
}
