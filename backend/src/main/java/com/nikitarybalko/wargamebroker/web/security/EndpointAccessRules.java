package com.nikitarybalko.wargamebroker.web.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the access rule of a controller method. A rule on the method overrides the rule on its controller class.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EndpointAccessRules {

    public static Optional<AccessRule> resolve(Method handlerMethod, Class<?> controllerType) {
        return declaredRule(handlerMethod).or(() -> declaredRule(controllerType));
    }

    private static Optional<AccessRule> declaredRule(AnnotatedElement element) {
        RequiresRole requiresRole = AnnotatedElementUtils.findMergedAnnotation(element, RequiresRole.class);
        boolean anyAuthenticated = AnnotatedElementUtils.hasAnnotation(element, AnyAuthenticatedRole.class);

        if (Objects.nonNull(requiresRole) && anyAuthenticated) {
            throw new IllegalStateException(element + " declares both @RequiresRole and @AnyAuthenticatedRole");
        }
        if (anyAuthenticated) {
            return Optional.of(AccessRule.anyAuthenticated());
        }
        return Optional.ofNullable(requiresRole).map(annotation -> AccessRule.restrictedTo(annotation.value()));
    }
}
