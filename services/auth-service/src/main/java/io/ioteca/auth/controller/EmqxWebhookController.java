package io.ioteca.auth.controller;

import io.ioteca.auth.dto.EmqxAclRequest;
import io.ioteca.auth.dto.EmqxAuthRequest;
import io.ioteca.auth.dto.EmqxAuthResponse;
import io.ioteca.auth.entity.ApiKey;
import io.ioteca.auth.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/internal/emqx")
@RequiredArgsConstructor
@Slf4j
public class EmqxWebhookController {

    private final ApiKeyService apiKeyService;

    /**
     * Called by EMQX on every device connect attempt.
     * username = project_id, password = full API key.
     */
    @PostMapping("/auth")
    public EmqxAuthResponse authenticate(@RequestBody EmqxAuthRequest req) {
        Optional<ApiKey> key = apiKeyService.validateKey(req.password());

        if (key.isEmpty()) {
            log.warn("EMQX auth denied: clientid={} peer={}", req.clientid(), req.peerhost());
            return EmqxAuthResponse.DENY;
        }

        // The username (project_id) must match the project embedded in the key
        String expectedProjectPrefix = req.password().split("_")[2]; // 3rd segment = project short id
        boolean projectMatches = key.get().getProjectId().toString()
                .replace("-", "").startsWith(expectedProjectPrefix);

        if (!projectMatches) {
            log.warn("EMQX auth denied: project mismatch clientid={}", req.clientid());
            return EmqxAuthResponse.DENY;
        }

        apiKeyService.touchLastUsed(key.get().getId());
        log.debug("EMQX auth allowed: clientid={} project={}", req.clientid(), key.get().getProjectId());
        return EmqxAuthResponse.ALLOW;
    }

    /**
     * Called by EMQX to authorize a device to publish/subscribe on a topic.
     * Topic format: IoTeca/{project_id}/{device_id}/{metric_group}
     * A device may only publish to its own project's topics.
     */
    @PostMapping("/acl")
    public EmqxAuthResponse authorize(@RequestBody EmqxAclRequest req) {
        // Only allow publish actions (subscribe is for server-side consumers)
        if (!"publish".equalsIgnoreCase(req.action())) {
            return EmqxAuthResponse.DENY;
        }

        // Topic must start with IoTeca/{project_id}/
        String[] parts = req.topic().split("/");
        if (parts.length < 3 || !"ioteca".equals(parts[0])) {
            return EmqxAuthResponse.DENY;
        }

        String topicProjectId = parts[1];
        // username is the project_id the device connected with
        boolean topicMatchesAuth = topicProjectId.equals(req.username());

        return topicMatchesAuth ? EmqxAuthResponse.ALLOW : EmqxAuthResponse.DENY;
    }

    /**
     * Internal token-validation endpoint for use by api-gateway and other services.
     */
    @GetMapping("/key/validate")
    public EmqxAuthResponse validateKey(@RequestParam String key) {
        return apiKeyService.validateKey(key).isPresent()
                ? EmqxAuthResponse.ALLOW : EmqxAuthResponse.DENY;
    }
}
