package com.nikitarybalko.wargamebroker.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces {@link RequiresRole} and {@link AnyAuthenticatedRole}. Endpoints without a declared rule are denied.
 */
@Slf4j
@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private static final String MISSING_RULE_MESSAGE = "Endpoint has no declared access rule";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            AccessRule rule = EndpointAccessRules.resolve(handlerMethod.getMethod(), handlerMethod.getBeanType())
                    .orElseThrow(() -> denyUndeclaredEndpoint(handlerMethod));
            SecurityUtils.requireRole(request, rule);
        }
        return true;
    }

    private ForbiddenException denyUndeclaredEndpoint(HandlerMethod handlerMethod) {
        log.error("Access denied to {}: {}", handlerMethod.getShortLogMessage(), MISSING_RULE_MESSAGE);
        return new ForbiddenException(MISSING_RULE_MESSAGE);
    }
}
