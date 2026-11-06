package com.nikitarybalko.wargamebroker.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikitarybalko.wargamebroker.config.ApiKeyProperties;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.web.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";
    public static final String ATTR_CLIENT_ROLE = "ATTR_CLIENT_ROLE";
    public static final String ATTR_CLIENT_SIDE = "ATTR_CLIENT_SIDE";

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/swagger-ui", "/v3/api-docs", "/error", "/favicon.ico");
    private static final String MISSING_KEY_MESSAGE = "Missing required " + API_KEY_HEADER + " header";
    private static final String INVALID_KEY_MESSAGE = "Invalid " + API_KEY_HEADER + " provided";

    private final Map<String, ClientRole> rolesByApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(ApiKeyProperties apiKeys, ObjectMapper objectMapper) {
        this.rolesByApiKey = Map.of(
                apiKeys.blueApiKey(), ClientRole.ROLE_BLUE,
                apiKeys.redApiKey(), ClientRole.ROLE_RED,
                apiKeys.adminApiKey(), ClientRole.ROLE_ADMIN);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isPublicPath(request.getRequestURI()) || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (Objects.isNull(apiKey) || apiKey.isBlank()) {
            respondUnauthorized(response, MISSING_KEY_MESSAGE);
            return;
        }

        ClientRole clientRole = rolesByApiKey.get(apiKey.trim());
        if (Objects.isNull(clientRole)) {
            respondUnauthorized(response, INVALID_KEY_MESSAGE);
            return;
        }

        request.setAttribute(ATTR_CLIENT_ROLE, clientRole);
        Side clientSide = resolveSide(clientRole);
        if (Objects.nonNull(clientSide)) {
            request.setAttribute(ATTR_CLIENT_SIDE, clientSide);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
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
