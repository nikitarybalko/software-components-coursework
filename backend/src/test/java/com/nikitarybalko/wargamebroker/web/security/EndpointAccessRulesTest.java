package com.nikitarybalko.wargamebroker.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit tests: EndpointAccessRules resolution")
class EndpointAccessRulesTest {

    private static final String UNANNOTATED_METHOD = "unannotated";
    private static final String OPEN_METHOD = "open";
    private static final String CONFLICTING_METHOD = "conflicting";
    private static final String EMPTY_ROLES_METHOD = "emptyRoles";

    @RequiresRole(ClientRole.ROLE_ADMIN)
    static class AdminController {

        public void unannotated() {
        }

        @AnyAuthenticatedRole
        public void open() {
        }
    }

    static class PlainController {

        public void unannotated() {
        }

        @RequiresRole(ClientRole.ROLE_ADMIN)
        @AnyAuthenticatedRole
        public void conflicting() {
        }

        @RequiresRole({})
        public void emptyRoles() {
        }
    }

    @Test
    @DisplayName("Class-level rule applies to methods without their own rule")
    void classRuleAppliesToUnannotatedMethod() {
        Optional<AccessRule> rule = resolve(AdminController.class, UNANNOTATED_METHOD);

        assertThat(rule).contains(AccessRule.restrictedTo(ClientRole.ROLE_ADMIN));
    }

    @Test
    @DisplayName("Method-level rule overrides the class-level rule")
    void methodRuleOverridesClassRule() {
        Optional<AccessRule> rule = resolve(AdminController.class, OPEN_METHOD);

        assertThat(rule).hasValueSatisfying(resolved ->
                assertThat(resolved.allowedRoles()).isEqualTo(EnumSet.allOf(ClientRole.class)));
    }

    @Test
    @DisplayName("Endpoint without any rule resolves to no rule")
    void undeclaredEndpointHasNoRule() {
        assertThat(resolve(PlainController.class, UNANNOTATED_METHOD)).isEmpty();
    }

    @Test
    @DisplayName("Declaring both annotations on one element is rejected")
    void conflictingAnnotationsAreRejected() {
        assertThatThrownBy(() -> resolve(PlainController.class, CONFLICTING_METHOD))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Empty @RequiresRole resolves to a rule that permits nobody")
    void emptyRolesPermitNobody() {
        Optional<AccessRule> rule = resolve(PlainController.class, EMPTY_ROLES_METHOD);

        assertThat(rule).hasValueSatisfying(resolved -> {
            assertThat(resolved.allowedRoles()).isEmpty();
            EnumSet.allOf(ClientRole.class).forEach(role -> assertThat(resolved.permits(role)).isFalse());
        });
    }

    private static Optional<AccessRule> resolve(Class<?> controller, String methodName) {
        try {
            Method method = controller.getDeclaredMethod(methodName);
            return EndpointAccessRules.resolve(method, controller);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
