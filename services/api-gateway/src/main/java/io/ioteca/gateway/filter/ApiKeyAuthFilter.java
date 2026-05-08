package io.ioteca.gateway.filter;

import io.ioteca.gateway.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * Runs before JwtAuthenticationFilter.
 * Detects API keys (Bearer tokens starting with "ioteca_"), validates them
 * against the database, and stores the resolved projectId as a request attribute
 * so IngestController can route the message to the correct project.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String PROJECT_ID_ATTR = "apiKeyProjectId";
    private static final String API_KEY_PREFIX = "ioteca_";

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer " + API_KEY_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String rawKey = authHeader.substring(7);
            String hash   = sha256Hex(rawKey);

            apiKeyRepository.findByKeyHashAndIsActiveTrue(hash)
                    .filter(k -> k.getExpiresAt() == null
                            || k.getExpiresAt().isAfter(OffsetDateTime.now()))
                    .ifPresent(key -> {
                        request.setAttribute(PROJECT_ID_ATTR, key.getProjectId());

                        var auth = new UsernamePasswordAuthenticationToken(
                                "device:" + key.getProjectId(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }

        chain.doFilter(request, response);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
