package io.ioteca.gateway.filter;

import io.ioteca.gateway.entity.ApiKey;
import io.ioteca.gateway.repository.ApiKeyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock ApiKeyRepository apiKeyRepository;
    @InjectMocks ApiKeyAuthFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain    = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── no / non-API-key header ───────────────────────────────────────────────

    @Test
    void noAuthHeader_passesChainUnauthenticated() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void jwtBearerHeader_isIgnored() throws Exception {
        request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig");
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(apiKeyRepository, never()).findByKeyHashAndIsActiveTrue(any());
    }

    // ── valid API key ─────────────────────────────────────────────────────────

    @Test
    void validApiKey_setsDeviceAuthentication() throws Exception {
        UUID projectId = UUID.randomUUID();
        ApiKey key = apiKey(projectId, null);
        when(apiKeyRepository.findByKeyHashAndIsActiveTrue(any())).thenReturn(Optional.of(key));

        request.addHeader("Authorization", "Bearer ioteca_live_abcd1234_somevalidkey");
        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).contains(projectId.toString());
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_DEVICE"));
    }

    @Test
    void validApiKey_storesProjectIdAttribute() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(apiKeyRepository.findByKeyHashAndIsActiveTrue(any()))
                .thenReturn(Optional.of(apiKey(projectId, null)));

        request.addHeader("Authorization", "Bearer ioteca_live_abcd1234_somevalidkey");
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest().getAttribute(ApiKeyAuthFilter.PROJECT_ID_ATTR))
                .isEqualTo(projectId);
    }

    // ── invalid / expired key ─────────────────────────────────────────────────

    @Test
    void unknownApiKey_leavesContextEmpty() throws Exception {
        when(apiKeyRepository.findByKeyHashAndIsActiveTrue(any())).thenReturn(Optional.empty());

        request.addHeader("Authorization", "Bearer ioteca_live_abcd1234_unknownkey");
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void expiredApiKey_leavesContextEmpty() throws Exception {
        when(apiKeyRepository.findByKeyHashAndIsActiveTrue(any()))
                .thenReturn(Optional.of(apiKey(UUID.randomUUID(), OffsetDateTime.now().minusHours(1))));

        request.addHeader("Authorization", "Bearer ioteca_live_abcd1234_expiredkey");
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void futureExpiryApiKey_isAccepted() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(apiKeyRepository.findByKeyHashAndIsActiveTrue(any()))
                .thenReturn(Optional.of(apiKey(projectId, OffsetDateTime.now().plusDays(30))));

        request.addHeader("Authorization", "Bearer ioteca_live_abcd1234_validkey");
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ApiKey apiKey(UUID projectId, OffsetDateTime expiresAt) throws Exception {
        ApiKey key = new ApiKey();
        setField(key, "projectId", projectId);
        setField(key, "expiresAt", expiresAt);
        return key;
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        var f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
