package io.afridata.auth.dto;

import io.afridata.auth.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String country,
        String role,
        boolean emailVerified,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(),
                u.getPhone(), u.getCountry(), u.getRole(),
                u.isEmailVerified(), u.getCreatedAt(), u.getLastLoginAt());
    }
}
