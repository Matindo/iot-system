package io.ioteca.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform")
public record PlatformConfig(String baseUrl, String fromEmail) {}
