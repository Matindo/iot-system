package io.ioteca.auth.service;

import io.ioteca.auth.config.JwtConfig;
import io.ioteca.auth.dto.LoginRequest;
import io.ioteca.auth.dto.RegisterRequest;
import io.ioteca.auth.dto.TokenRefreshRequest;
import io.ioteca.auth.entity.SubscriptionTier;
import io.ioteca.auth.entity.User;
import io.ioteca.auth.repository.SubscriptionTierRepository;
import io.ioteca.auth.repository.UserRepository;
import io.ioteca.auth.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserSubscriptionRepository subscriptionRepository;
    @Mock SubscriptionTierRepository tierRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    private SubscriptionTier freeTier;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(new JwtConfig("test-secret-key-32-chars-minimum!", 60L, 30L));
        authService = new AuthService(userRepository, subscriptionRepository, tierRepository,
                passwordEncoder, jwtService);

        freeTier = SubscriptionTier.builder().id(1).name("FREE").maxMessagesPerDay(10_000L).build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_createsUserAndReturnsTokens() {
        when(userRepository.existsByEmail("new@ioteca.io")).thenReturn(false);
        when(tierRepository.findByNameAndIsActiveTrue("FREE")).thenReturn(Optional.of(freeTier));
        User saved = User.builder().id(UUID.randomUUID()).email("new@ioteca.io").role("USER").build();
        when(userRepository.save(any())).thenReturn(saved);
        when(subscriptionRepository.save(any())).thenReturn(null);

        var resp = authService.register(new RegisterRequest("new@ioteca.io", "Pass123!", "Tester", null, "KE"));

        assertThat(resp.accessToken()).isNotBlank();
        assertThat(resp.refreshToken()).isNotBlank();
        assertThat(resp.email()).isEqualTo("new@ioteca.io");
        assertThat(resp.role()).isEqualTo("USER");
    }

    @Test
    void register_lowercasesEmail() {
        // existsByEmail is called with the raw (pre-lowercase) value from the request
        when(userRepository.existsByEmail("UPPER@ioteca.io")).thenReturn(false);
        when(tierRepository.findByNameAndIsActiveTrue("FREE")).thenReturn(Optional.of(freeTier));
        User saved = User.builder().id(UUID.randomUUID()).email("upper@ioteca.io").role("USER").build();
        when(userRepository.save(any())).thenReturn(saved);
        when(subscriptionRepository.save(any())).thenReturn(null);

        authService.register(new RegisterRequest("UPPER@ioteca.io", "Pass123!", null, null, "KE"));

        verify(userRepository).save(argThat(u -> "upper@ioteca.io".equals(u.getEmail())));
    }

    @Test
    void register_throwsOnDuplicateEmail() {
        when(userRepository.existsByEmail("existing@ioteca.io")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("existing@ioteca.io", "Pass123!", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_defaultsCountryToKE() {
        when(userRepository.existsByEmail("ke@ioteca.io")).thenReturn(false);
        when(tierRepository.findByNameAndIsActiveTrue("FREE")).thenReturn(Optional.of(freeTier));
        User saved = User.builder().id(UUID.randomUUID()).email("ke@ioteca.io").role("USER").build();
        when(userRepository.save(any())).thenReturn(saved);
        when(subscriptionRepository.save(any())).thenReturn(null);

        authService.register(new RegisterRequest("ke@ioteca.io", "Pass123!", null, null, null));

        verify(userRepository).save(argThat(u -> "KE".equals(u.getCountry())));
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_returnsTokensForValidCredentials() {
        String rawPw = "correct-horse-battery-staple";
        User user = User.builder().id(UUID.randomUUID()).email("user@ioteca.io")
                .role("USER").isActive(true)
                .passwordHash(passwordEncoder.encode(rawPw)).build();
        when(userRepository.findByEmail("user@ioteca.io")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        var resp = authService.login(new LoginRequest("user@ioteca.io", rawPw));

        assertThat(resp.accessToken()).isNotBlank();
        assertThat(resp.email()).isEqualTo("user@ioteca.io");
    }

    @Test
    void login_throwsForWrongPassword() {
        User user = User.builder().id(UUID.randomUUID()).email("user@ioteca.io")
                .isActive(true).passwordHash(passwordEncoder.encode("correct")).build();
        when(userRepository.findByEmail("user@ioteca.io")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@ioteca.io", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsForInactiveUser() {
        User user = User.builder().id(UUID.randomUUID()).email("user@ioteca.io")
                .isActive(false).passwordHash(passwordEncoder.encode("pw")).build();
        when(userRepository.findByEmail("user@ioteca.io")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@ioteca.io", "pw")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void login_throwsForUnknownEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@ioteca.io", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_returnsNewAccessTokenForValidRefreshToken() {
        User user = User.builder().id(UUID.randomUUID()).email("user@ioteca.io")
                .role("USER").isActive(true).build();
        String refreshToken = jwtService.generateRefreshToken(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var resp = authService.refresh(new TokenRefreshRequest(refreshToken));

        assertThat(resp.accessToken()).isNotBlank();
    }

    @Test
    void refresh_throwsForAccessToken() {
        User user = User.builder().id(UUID.randomUUID()).email("user@ioteca.io")
                .role("USER").build();
        String accessToken = jwtService.generateAccessToken(user);

        assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(accessToken)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_throwsForGarbageToken() {
        assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest("not.a.token")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_throwsForDeactivatedUser() {
        User inactive = User.builder().id(UUID.randomUUID()).email("x@ioteca.io")
                .role("USER").isActive(false).build();
        // Build the refresh token with a service that knows the secret
        String refreshToken = jwtService.generateRefreshToken(inactive);
        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(refreshToken)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("deactivated");
    }
}
