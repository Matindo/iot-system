package io.ioteca.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;

    public void post(String url, Map<String, Object> payload) {
        if (url == null || url.isBlank()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("Webhook delivered: url={} status={}", url, response.getStatusCode());
        } catch (Exception e) {
            log.error("Webhook failed: url={} error={}", url, e.getMessage());
        }
    }
}
