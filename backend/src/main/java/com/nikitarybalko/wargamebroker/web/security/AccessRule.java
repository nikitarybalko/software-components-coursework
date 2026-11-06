package com.nikitarybalko.wargamebroker.web.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record AccessRule(Set<ClientRole> allowedRoles) {

    public AccessRule {
        allowedRoles = Collections.unmodifiableSet(allowedRoles.isEmpty()
                ? EnumSet.noneOf(ClientRole.class)
                : EnumSet.copyOf(allowedRoles));
    }

    public static AccessRule anyAuthenticated() {
        return new AccessRule(EnumSet.allOf(ClientRole.class));
    }

    public static AccessRule restrictedTo(ClientRole... roles) {
        return new AccessRule(Set.copyOf(Arrays.asList(roles)));
    }

    public boolean permits(ClientRole role) {
        return allowedRoles.contains(role);
    }
}
