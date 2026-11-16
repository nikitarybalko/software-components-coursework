package com.nikitarybalko.wargamebroker.domain.exception;

public class InvalidTickException extends SimulationException {
    public InvalidTickException(long expected, long actual) {
        super("Invalid tick number. Expected: " + expected + ", actual: " + actual);
    }
}
