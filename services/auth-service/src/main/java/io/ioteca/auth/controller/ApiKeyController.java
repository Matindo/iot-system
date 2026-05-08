package io.ioteca.auth.controller;

import io.ioteca.auth.dto.ApiKeyCreateRequest;
import io.ioteca.auth.dto.ApiKeyCreatedResponse;
import io.ioteca.auth.dto.ApiKeyResponse;
import io.ioteca.auth.service.ApiKeyService;
import io.ioteca.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> createKey(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ApiKeyCreateRequest req) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.createKey(userId, req));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listKeys(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam UUID projectId) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(apiKeyService.listKeys(userId, projectId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "User revoked") String reason) {
        UUID userId = extractUserId(authHeader);
        apiKeyService.revokeKey(userId, id, reason);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(String authHeader) {
        return jwtService.extractUserId(authHeader.substring(7));
    }
}
