package io.ioteca.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmqxAuthRequest(
        String username,
        String password,
        String clientid,
        String peerhost,
        @JsonProperty("proto_name") String protoName,
        String mountpoint
) {}
