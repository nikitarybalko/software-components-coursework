package com.nikitarybalko.wargamebroker.domain.exception;

public class TickNotCompleteException extends SimulationException {
    public TickNotCompleteException(long tickNumber) {
        super("Tick " + tickNumber + " is not complete yet. Waiting for both sides to submit.");
    }
}
