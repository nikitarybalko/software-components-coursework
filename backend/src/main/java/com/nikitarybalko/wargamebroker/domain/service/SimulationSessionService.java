package com.nikitarybalko.wargamebroker.domain.service;

import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;

import java.util.UUID;

public interface SimulationSessionService {

    void initSimulation();

    void resetSimulation();

    void pauseSimulation(String reason);

    void resumeSimulation();

    void restoreFromSnapshot(UUID snapshotId);

    SimulationStatus getStatus();
}
