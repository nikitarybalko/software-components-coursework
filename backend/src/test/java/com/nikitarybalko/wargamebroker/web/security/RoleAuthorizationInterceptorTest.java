package com.nikitarybalko.wargamebroker.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit tests: RoleAuthorizationInterceptor")
class RoleAuthorizationInterceptorTest {

    private static final String ADMIN_ONLY_METHOD = "adminOnly";
    private static final String OPEN_METHOD = "open";
    private static final String UNDECLARED_METHOD = "undeclared";

    private final RoleAuthorizationInterceptor interceptor = new RoleAuthorizationInterceptor();

    static class FixtureController {

        @RequiresRole(ClientRole.ROLE_ADMIN)
        public void adminOnly() {
        }

        @AnyAuthenticatedRole
        public void open() {
        }

        public void undeclared() {
        }
    }

    @Test
    @DisplayName("Allowed role passes the role check")
    void allowedRolePasses() throws Exception {
        assertThat(preHandle(ADMIN_ONLY_METHOD, ClientRole.ROLE_ADMIN)).isTrue();
    }

    @Test
    @DisplayName("Role outside @RequiresRole is rejected with ForbiddenException")
    void disallowedRoleIsRejected() {
        assertThatThrownBy(() -> preHandle(ADMIN_ONLY_METHOD, ClientRole.ROLE_BLUE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("@AnyAuthenticatedRole endpoint accepts every authenticated role")
    void openEndpointAcceptsEveryRole() throws Exception {
        for (ClientRole role : ClientRole.values()) {
            assertThat(preHandle(OPEN_METHOD, role)).isTrue();
        }
    }

    @Test
    @DisplayName("Endpoint without a declared rule is denied")
    void undeclaredEndpointIsDenied() {
        assertThatThrownBy(() -> preHandle(UNDECLARED_METHOD, ClientRole.ROLE_ADMIN))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Unauthenticated request to a protected endpoint is rejected with UnauthorizedException")
    void unauthenticatedRequestIsRejected() {
        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(),
                handler(ADMIN_ONLY_METHOD)))
                .isInstanceOf(UnauthorizedException.class);
    }

    private boolean preHandle(String methodName, ClientRole role) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(ApiKeyAuthFilter.ATTR_CLIENT_ROLE, role);
        return interceptor.preHandle(request, new MockHttpServletResponse(), handler(methodName));
    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new FixtureController(), methodName);
    }
}
