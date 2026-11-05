package com.nikitarybalko.wargamebroker.web.security;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
