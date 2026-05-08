package io.ioteca.quota.service;

import io.ioteca.quota.entity.SubscriptionTier;
import io.ioteca.quota.entity.UserSubscription;
import io.ioteca.quota.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaService {

    private final StringRedisTemplate redis;
    private final UserSubscriptionRepository subscriptionRepository;

    private static final long COUNTER_TTL_SECONDS = 172_800; // 2 days

    /**
     * Increments the daily message counter for the project and returns
     * the quota check result: OK | WARNING_80 | EXCEEDED.
     */
    public QuotaStatus increment(UUID projectId) {
        String key = dailyKey(projectId);
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            log.warn("Redis increment returned null for key {}", key);
            return QuotaStatus.OK;
        }

        // Set TTL on first write; subsequent calls are no-ops for TTL
        if (count == 1) {
            redis.expire(key, Duration.ofSeconds(COUNTER_TTL_SECONDS));
        }

        long limit = dailyLimit(projectId);
        if (limit < 0) return QuotaStatus.OK; // unlimited tier

        if (count >= limit) return QuotaStatus.EXCEEDED;
        if (count >= (long) (limit * 0.8)) return QuotaStatus.WARNING_80;
        return QuotaStatus.OK;
    }

    public long currentCount(UUID projectId) {
        String val = redis.opsForValue().get(dailyKey(projectId));
        return val == null ? 0L : Long.parseLong(val);
    }

    public long dailyLimit(UUID projectId) {
        return subscriptionRepository.findActiveForProject(projectId)
                .map(UserSubscription::getTier)
                .map(SubscriptionTier::getMaxMessagesPerDay)
                .orElse(10_000L); // fallback to FREE tier limit
    }

    private String dailyKey(UUID projectId) {
        return String.format("project:%s:msgs:%s", projectId, LocalDate.now());
    }

    public enum QuotaStatus { OK, WARNING_80, EXCEEDED }
}
