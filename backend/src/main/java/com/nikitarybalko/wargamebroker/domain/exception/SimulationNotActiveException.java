package com.nikitarybalko.wargamebroker.domain.exception;

import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;

public class SimulationNotActiveException extends SimulationException {
    public SimulationNotActiveException(SimulationStatus currentStatus) {
        super("Simulation is not active. Current status: " + currentStatus);
    }
}
