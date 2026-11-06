package com.nikitarybalko.wargamebroker.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * API keys configured in application-test.yml.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestApiKeys {

    public static final String BLUE = "blue-secret-token";
    public static final String RED = "red-secret-token";
    public static final String ADMIN = "admin-secret-token";
    public static final String INVALID = "invalid-key";
}
