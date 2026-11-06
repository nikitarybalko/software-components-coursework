package com.nikitarybalko.wargamebroker.web.security;

import com.nikitarybalko.wargamebroker.WargameBrokerApplication;
import com.nikitarybalko.wargamebroker.web.controller.SimulationController;
import com.nikitarybalko.wargamebroker.web.controller.SimulatorController;
import com.nikitarybalko.wargamebroker.web.controller.StateController;
import com.nikitarybalko.wargamebroker.web.controller.TickController;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails the build when a new endpoint forgets to declare who may call it.
 */
@DisplayName("Security tests: every endpoint declares an access rule")
class EndpointAccessCoverageTest {

    private static final String BASE_PACKAGE = WargameBrokerApplication.class.getPackageName();

    @Test
    @DisplayName("Controller scan finds all known REST controllers")
    void scanFindsKnownControllers() {
        assertThat(restControllers()).contains(
                SimulationController.class, SimulatorController.class, StateController.class, TickController.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("handlerMethods")
    @DisplayName("Endpoint declares exactly one non-empty access rule")
    void endpointDeclaresNonEmptyAccessRule(Method handlerMethod) {
        Optional<AccessRule> rule = EndpointAccessRules.resolve(handlerMethod, handlerMethod.getDeclaringClass());

        assertThat(rule)
                .as("annotate %s with @RequiresRole or @AnyAuthenticatedRole", handlerMethod)
                .isPresent();
        assertThat(rule.get().allowedRoles())
                .as("@RequiresRole on %s must list at least one role", handlerMethod)
                .isNotEmpty();
    }

    static Stream<Named<Method>> handlerMethods() {
        return restControllers().stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class))
                .map(method -> Named.of(method.getDeclaringClass().getSimpleName() + "." + method.getName(), method));
    }

    private static List<Class<?>> restControllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .map(definition -> ClassUtils.resolveClassName(definition.getBeanClassName(), null))
                .<Class<?>>map(type -> type)
                .toList();
    }
}
