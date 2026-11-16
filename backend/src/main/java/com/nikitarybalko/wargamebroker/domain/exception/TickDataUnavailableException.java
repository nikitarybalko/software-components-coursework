package com.nikitarybalko.wargamebroker.domain.exception;

public class TickDataUnavailableException extends SimulationException {
    public TickDataUnavailableException(long tickNumber) {
        super("Visibility data for tick " + tickNumber + " is no longer available");
    }
}
