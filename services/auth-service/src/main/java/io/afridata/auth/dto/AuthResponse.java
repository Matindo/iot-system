package io.afridata.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        String role
) {
    public static AuthResponse of(String access, String refresh, long expiresIn,
                                  UUID userId, String email, String role) {
        return new AuthResponse(access, refresh, "Bearer", expiresIn, userId, email, role);
    }
}
