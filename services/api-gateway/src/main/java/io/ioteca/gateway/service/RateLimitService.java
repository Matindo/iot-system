package io.ioteca.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Token-bucket rate limiter backed by Redis.
 * Key: ratelimit:{userId}:api â€” counter of API calls in current minute.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    private static final int DEFAULT_LIMIT_PER_MINUTE = 60;

    public boolean isAllowed(UUID userId, int limitPerMinute) {
        String key = "ratelimit:" + userId + ":api";
        Long count = redis.opsForValue().increment(key);
        if (count == null) return true;
        if (count == 1) {
            redis.expire(key, Duration.ofMinutes(1));
        }
        return count <= limitPerMinute;
    }

    public boolean isAllowed(UUID userId) {
        return isAllowed(userId, DEFAULT_LIMIT_PER_MINUTE);
    }
}
