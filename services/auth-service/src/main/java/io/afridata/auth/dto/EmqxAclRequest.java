package io.afridata.auth.dto;

public record EmqxAclRequest(
        String username,
        String clientid,
        String topic,
        String action,
        String peerhost
) {}
