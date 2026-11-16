package com.nikitarybalko.wargamebroker.domain.exception;

import com.nikitarybalko.wargamebroker.domain.model.Side;

public class SimulatorNotRegisteredException extends SimulationException {
    public SimulatorNotRegisteredException(Side side) {
        super("Simulator for side " + side + " is not registered. Call the registration endpoint first.");
    }
}
