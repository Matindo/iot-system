package io.ioteca.quota.service;

import io.ioteca.quota.entity.SubscriptionTier;
import io.ioteca.quota.entity.UserSubscription;
import io.ioteca.quota.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;
    @Mock UserSubscriptionRepository subscriptionRepository;
    @InjectMocks QuotaService quotaService;

    private UUID projectId;
    private UserSubscription subscription;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        lenient().when(redis.opsForValue()).thenReturn(valueOps);

        SubscriptionTier freeTier = new SubscriptionTier();
        freeTier.setMaxMessagesPerDay(10_000L);
        subscription = new UserSubscription();
        subscription.setTier(freeTier);
    }

    // ── status thresholds ─────────────────────────────────────────────────────

    @Test
    void increment_returnsOkWellBelowLimit() {
        when(valueOps.increment(any())).thenReturn(1_000L);
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(subscription));

        assertThat(quotaService.increment(projectId)).isEqualTo(QuotaService.QuotaStatus.OK);
    }

    @Test
    void increment_returnsWarning80AtExactlyEightyPercent() {
        when(valueOps.increment(any())).thenReturn(8_000L); // exactly 80% of 10000
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(subscription));

        assertThat(quotaService.increment(projectId)).isEqualTo(QuotaService.QuotaStatus.WARNING_80);
    }

    @Test
    void increment_returnsWarning80JustBeforeLimit() {
        when(valueOps.increment(any())).thenReturn(9_999L);
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(subscription));

        assertThat(quotaService.increment(projectId)).isEqualTo(QuotaService.QuotaStatus.WARNING_80);
    }

    @Test
    void increment_returnsExceededAtLimit() {
        when(valueOps.increment(any())).thenReturn(10_000L);
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(subscription));

        assertThat(quotaService.increment(projectId)).isEqualTo(QuotaService.QuotaStatus.EXCEEDED);
    }

    @Test
    void increment_returnsExceededAboveLimit() {
        when(valueOps.increment(any())).thenReturn(15_000L);
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(subscription));

        assertThat(quotaService.increment(projectId)).isEqualTo(QuotaService.QuotaStatus.EXCEEDED);
    }

    // ── unlimited tier ────────────────────────────────────────────────────────

    @Test
    void increment_unlimitedTierAlwaysOk() {
        SubscriptionTier enterprise = new SubscriptionTier();
        enterprise.setMaxMessagesPerDay(-1L);
        UserSubscription sub = new UserSubscription();
        sub.setTier(enterprise);

        when(valueOps.increment(any())).thenReturn(99_999_999L);
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(sub));

        assertThat(quotaService.increment(projectId)).isEqualTo(QuotaService.QuotaStatus.OK);
    }

    // ── redis resilience ──────────────────────────────────────────────────────

    @Test
    void increment_returnsOkWhenRedisReturnsNull() {
        when(valueOps.increment(any())).thenReturn(null);

        assertThat(quotaService.increment(projectId)).isEqualTo(QuotaService.QuotaStatus.OK);
    }

    // ── TTL set on first write ────────────────────────────────────────────────

    @Test
    void increment_setsTtlOnFirstWrite() {
        when(valueOps.increment(any())).thenReturn(1L); // first write
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(subscription));

        quotaService.increment(projectId);

        verify(redis).expire(any(), any());
    }

    @Test
    void increment_doesNotSetTtlOnSubsequentWrites() {
        when(valueOps.increment(any())).thenReturn(50L); // not first write
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.of(subscription));

        quotaService.increment(projectId);

        verify(redis, never()).expire(any(), any());
    }

    // ── fallback to FREE tier limit ───────────────────────────────────────────

    @Test
    void dailyLimit_fallsBackToFreeTierWhenNoSubscription() {
        when(subscriptionRepository.findActiveForProject(projectId))
                .thenReturn(Optional.empty());

        assertThat(quotaService.dailyLimit(projectId)).isEqualTo(10_000L);
    }

    // ── currentCount ─────────────────────────────────────────────────────────

    @Test
    void currentCount_returnsZeroWhenKeyAbsent() {
        when(valueOps.get(any())).thenReturn(null);

        assertThat(quotaService.currentCount(projectId)).isEqualTo(0L);
    }

    @Test
    void currentCount_returnsStoredValue() {
        when(valueOps.get(any())).thenReturn("4250");

        assertThat(quotaService.currentCount(projectId)).isEqualTo(4250L);
    }
}
