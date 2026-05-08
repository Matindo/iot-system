package io.ioteca.auth.dto;

public record EmqxAuthResponse(String result) {
    public static final EmqxAuthResponse ALLOW = new EmqxAuthResponse("allow");
    public static final EmqxAuthResponse DENY  = new EmqxAuthResponse("deny");
}
