package com.nikitarybalko.wargamebroker.web.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Response codes and shared descriptions for OpenAPI annotations, which accept only compile-time constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ApiResponseDocs {

    static final String OK = "200";
    static final String BAD_REQUEST = "400";
    static final String UNAUTHORIZED = "401";
    static final String FORBIDDEN = "403";
    static final String NOT_FOUND = "404";
    static final String CONFLICT = "409";

    static final String INVALID_API_KEY = "Missing or invalid API key";
    static final String ADMIN_KEY_REQUIRED = "Admin API key required";
    static final String ADMIN_SIDE_REQUIRED = "Admin request without side parameter";
}
