package io.ioteca.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock RestTemplate restTemplate;
    @InjectMocks WebhookService webhookService;

    // ── null / blank URL guard ────────────────────────────────────────────────

    @Test
    void post_skipsNullUrl() {
        webhookService.post(null, Map.of("key", "value"));
        verify(restTemplate, never()).postForEntity(any(String.class), any(), any());
    }

    @Test
    void post_skipsBlankUrl() {
        webhookService.post("   ", Map.of("key", "value"));
        verify(restTemplate, never()).postForEntity(any(String.class), any(), any());
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void post_postsJsonPayloadToUrl() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        webhookService.post("https://example.com/hook", Map.of("alert", "fired"));

        verify(restTemplate).postForEntity(eq("https://example.com/hook"), any(), eq(String.class));
    }

    // ── resilience ────────────────────────────────────────────────────────────

    @Test
    void post_doesNotThrow_whenRestTemplateFails() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatNoException().isThrownBy(() ->
                webhookService.post("https://example.com/hook", Map.of()));
    }
}
