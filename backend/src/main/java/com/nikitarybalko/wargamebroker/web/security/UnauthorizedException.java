package com.nikitarybalko.wargamebroker.web.security;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
