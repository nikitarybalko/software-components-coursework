package com.nikitarybalko.wargamebroker.domain.service;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;

import java.util.UUID;

public interface SimulationSessionService {

    void registerSimulator(Side side);

    void initSimulation();

    void resetSimulation();

    void pauseSimulation(String reason);

    void resumeSimulation();

    void restoreFromSnapshot(UUID snapshotId);

    SimulationStatus getStatus();
}
