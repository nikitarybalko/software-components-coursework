package com.nikitarybalko.wargamebroker.web.security;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static ClientRole getAuthenticatedRole(HttpServletRequest request) {
        Object role = request.getAttribute(ApiKeyAuthFilter.ATTR_CLIENT_ROLE);
        if (Objects.isNull(role)) {
            throw new UnauthorizedException("Request is not authenticated");
        }
        return (ClientRole) role;
    }

    public static Side getAuthenticatedSide(HttpServletRequest request) {
        Object side = request.getAttribute(ApiKeyAuthFilter.ATTR_CLIENT_SIDE);
        return (Side) side;
    }

    public static void requireRole(HttpServletRequest request, AccessRule rule) {
        ClientRole currentRole = getAuthenticatedRole(request);
        if (!rule.permits(currentRole)) {
            throw new ForbiddenException("Role " + currentRole + " is not permitted to perform this operation");
        }
    }

    public static void validateSideAccess(HttpServletRequest request, Side targetSide) {
        ClientRole currentRole = getAuthenticatedRole(request);
        if (currentRole == ClientRole.ROLE_ADMIN) {
            return;
        }

        Side callerSide = getAuthenticatedSide(request);
        if (Objects.isNull(callerSide) || callerSide != targetSide) {
            throw new ForbiddenException("Faction " + currentRole + " cannot submit or inspect data for " + targetSide);
        }
    }
}
