package com.nikitarybalko.wargamebroker.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            resolveRequiredRoles(handlerMethod)
                    .ifPresent(roles -> SecurityUtils.requireRole(request, roles));
        }
        return true;
    }

    private Optional<ClientRole[]> resolveRequiredRoles(HandlerMethod handlerMethod) {
        return Optional.ofNullable(handlerMethod.getMethodAnnotation(RequiresRole.class))
                .or(() -> Optional.ofNullable(
                        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequiresRole.class)))
                .map(RequiresRole::value);
    }
}
