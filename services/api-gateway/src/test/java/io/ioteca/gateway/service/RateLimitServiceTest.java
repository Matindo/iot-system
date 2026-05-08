package io.ioteca.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks RateLimitService rateLimitService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    // ── allow / deny ──────────────────────────────────────────────────────────

    @Test
    void isAllowed_returnsTrueWellBelowLimit() {
        when(valueOps.increment(any())).thenReturn(30L);
        assertThat(rateLimitService.isAllowed(userId, 60)).isTrue();
    }

    @Test
    void isAllowed_returnsTrueAtExactLimit() {
        when(valueOps.increment(any())).thenReturn(60L);
        assertThat(rateLimitService.isAllowed(userId, 60)).isTrue();
    }

    @Test
    void isAllowed_returnsFalseAboveLimit() {
        when(valueOps.increment(any())).thenReturn(61L);
        assertThat(rateLimitService.isAllowed(userId, 60)).isFalse();
    }

    // ── Redis resilience ──────────────────────────────────────────────────────

    @Test
    void isAllowed_returnsTrueWhenRedisReturnsNull() {
        when(valueOps.increment(any())).thenReturn(null);
        assertThat(rateLimitService.isAllowed(userId, 60)).isTrue();
    }

    // ── TTL ───────────────────────────────────────────────────────────────────

    @Test
    void isAllowed_setsTtlOnFirstWrite() {
        when(valueOps.increment(any())).thenReturn(1L);
        rateLimitService.isAllowed(userId, 60);
        verify(redis).expire(any(), any());
    }

    @Test
    void isAllowed_doesNotSetTtlOnSubsequentWrites() {
        when(valueOps.increment(any())).thenReturn(5L);
        rateLimitService.isAllowed(userId, 60);
        verify(redis, never()).expire(any(), any());
    }

    // ── default limit overload ────────────────────────────────────────────────

    @Test
    void isAllowed_defaultLimitIs60() {
        when(valueOps.increment(any())).thenReturn(60L);
        assertThat(rateLimitService.isAllowed(userId)).isTrue();

        when(valueOps.increment(any())).thenReturn(61L);
        assertThat(rateLimitService.isAllowed(userId)).isFalse();
    }

    // ── key includes userId ───────────────────────────────────────────────────

    @Test
    void isAllowed_keyContainsUserId() {
        when(valueOps.increment(any())).thenReturn(1L);
        rateLimitService.isAllowed(userId);
        verify(valueOps).increment(contains(userId.toString()));
    }
}
