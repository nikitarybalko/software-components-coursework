package com.nikitarybalko.wargamebroker.domain.exception;

import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;

public class InvalidLifecycleTransitionException extends SimulationException {
    public InvalidLifecycleTransitionException(String action, SimulationStatus currentStatus) {
        super("Cannot " + action + " simulation in status " + currentStatus);
    }
}
