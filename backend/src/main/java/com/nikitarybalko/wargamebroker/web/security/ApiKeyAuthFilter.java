package com.nikitarybalko.wargamebroker.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.web.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;

@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";
    public static final String ATTR_CLIENT_ROLE = "ATTR_CLIENT_ROLE";
    public static final String ATTR_CLIENT_SIDE = "ATTR_CLIENT_SIDE";

    private final String blueApiKey;
    private final String redApiKey;
    private final String adminApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(
            @Value("${wargame.security.blue-api-key:blue-secret-token}") String blueApiKey,
            @Value("${wargame.security.red-api-key:red-secret-token}") String redApiKey,
            @Value("${wargame.security.admin-api-key:admin-secret-token}") String adminApiKey,
            ObjectMapper objectMapper) {
        this.blueApiKey = blueApiKey;
        this.redApiKey = redApiKey;
        this.adminApiKey = adminApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return isWhitelisted(path) || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (Objects.isNull(apiKey) || apiKey.isBlank()) {
            respondUnauthorized(response, "Missing required X-API-KEY header");
            return;
        }

        ClientRole clientRole = resolveRole(apiKey.trim());
        if (Objects.isNull(clientRole)) {
            respondUnauthorized(response, "Invalid X-API-KEY provided");
            return;
        }

        Side clientSide = resolveSide(clientRole);
        request.setAttribute(ATTR_CLIENT_ROLE, clientRole);
        if (Objects.nonNull(clientSide)) {
            request.setAttribute(ATTR_CLIENT_SIDE, clientSide);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.equals("/error")
                || path.equals("/favicon.ico");
    }

    private ClientRole resolveRole(String apiKey) {
        if (apiKey.equals(adminApiKey)) {
            return ClientRole.ROLE_ADMIN;
        }
        if (apiKey.equals(blueApiKey)) {
            return ClientRole.ROLE_BLUE;
        }
        if (apiKey.equals(redApiKey)) {
            return ClientRole.ROLE_RED;
        }
        return null;
    }

    private Side resolveSide(ClientRole role) {
        return switch (role) {
            case ROLE_BLUE -> Side.BLUE;
            case ROLE_RED -> Side.RED;
            case ROLE_ADMIN -> null;
        };
    }

    private void respondUnauthorized(HttpServletResponse response, String message) throws IOException {
        log.warn("Unauthorized request rejected: {}", message);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(message)
                .details(Collections.emptyList())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
