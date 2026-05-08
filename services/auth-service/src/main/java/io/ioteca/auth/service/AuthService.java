package io.ioteca.auth.service;

import io.ioteca.auth.dto.AuthResponse;
import io.ioteca.auth.dto.LoginRequest;
import io.ioteca.auth.dto.RegisterRequest;
import io.ioteca.auth.dto.TokenRefreshRequest;
import io.ioteca.auth.entity.SubscriptionTier;
import io.ioteca.auth.entity.User;
import io.ioteca.auth.entity.UserSubscription;
import io.ioteca.auth.repository.SubscriptionTierRepository;
import io.ioteca.auth.repository.UserRepository;
import io.ioteca.auth.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionTierRepository tierRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(req.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .country(req.country() != null ? req.country() : "KE")
                .build();
        user = userRepository.save(user);

        // Assign FREE tier subscription
        SubscriptionTier freeTier = tierRepository.findByNameAndIsActiveTrue("FREE")
                .orElseThrow(() -> new IllegalStateException("FREE tier not seeded in database"));
        subscriptionRepository.save(UserSubscription.builder()
                .userId(user.getId())
                .tier(freeTier)
                .status("ACTIVE")
                .build());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(TokenRefreshRequest req) {
        String token = req.refreshToken();
        if (!jwtService.isTokenValid(token) || !jwtService.isRefreshToken(token)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        return buildAuthResponse(user);
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AuthResponse buildAuthResponse(User user) {
        String access  = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        return AuthResponse.of(access, refresh,
                jwtService.accessTokenExpirySeconds(),
                user.getId(), user.getEmail(), user.getRole());
    }
}
