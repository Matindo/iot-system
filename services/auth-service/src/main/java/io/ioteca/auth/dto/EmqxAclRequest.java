package io.ioteca.auth.dto;

public record EmqxAclRequest(
        String username,
        String clientid,
        String topic,
        String action,
        String peerhost
) {}
