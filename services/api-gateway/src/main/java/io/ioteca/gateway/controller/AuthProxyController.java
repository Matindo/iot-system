package io.ioteca.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

@RestController
@RequiredArgsConstructor
public class AuthProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.auth-service-url}")
    private String authServiceUrl;

    @RequestMapping({"/api/v1/auth/**", "/api/v1/keys/**"})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String target = authServiceUrl + uri + (query != null ? "?" + query : "");

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.equalsIgnoreCase("host")
                    || name.equalsIgnoreCase("transfer-encoding")
                    || name.equalsIgnoreCase("content-length")) continue;
            headers.addAll(name, Collections.list(request.getHeaders(name)));
        }

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(
                    URI.create(target),
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    byte[].class
            );
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        }
    }
}
