package io.ioteca.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
        String secret,
        long expiryMinutes,
        long refreshExpiryDays
) {}
