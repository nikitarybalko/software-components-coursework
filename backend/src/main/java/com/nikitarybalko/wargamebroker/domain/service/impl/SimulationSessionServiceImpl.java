package com.nikitarybalko.wargamebroker.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.nikitarybalko.wargamebroker.domain.exception.SimulationException;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationSessionServiceImpl implements SimulationSessionService {

    private final TickManager tickManager;
    private final StateCheckpointingService checkpointingService;

    @Override
    public void initSimulation() {
        tickManager.reset();
        tickManager.setStatus(SimulationStatus.ACTIVE);
        checkpointingService.recordEvent(EventType.SIMULATION_INITIALIZED, null, "Simulation session initialized and activated");
        log.info("Simulation initialized and activated");
    }

    @Override
    public void resetSimulation() {
        tickManager.reset();
        tickManager.setStatus(SimulationStatus.INITIALIZING);
        checkpointingService.recordEvent(EventType.SIMULATION_RESET, null, "Simulation reset to initial state");
        log.info("Simulation reset");
    }

    @Override
    public void pauseSimulation(String reason) {
        tickManager.setStatus(SimulationStatus.PAUSED);
        String msg = reason != null ? reason : "User requested pause";
        checkpointingService.recordEvent(EventType.SIMULATION_PAUSED, null, msg);
        log.info("Simulation paused: {}", msg);
    }

    @Override
    public void resumeSimulation() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        checkpointingService.recordEvent(EventType.SIMULATION_RESUMED, null, "Simulation session resumed");
        log.info("Simulation resumed");
    }

    @Override
    public void restoreFromSnapshot(UUID snapshotId) {
        Snapshot snapshot = checkpointingService.loadSnapshot(snapshotId)
                .orElseThrow(() -> new SimulationException("Snapshot not found with ID: " + snapshotId));

        tickManager.restoreState(snapshot.getTickNumber(), snapshot.getUnits());
        checkpointingService.recordEvent(EventType.SNAPSHOT_RESTORED, null,
                "Restored simulation state from snapshot " + snapshotId + " at tick " + snapshot.getTickNumber());
        log.info("Successfully restored simulation state from snapshot {}", snapshotId);
    }

    @Override
    public SimulationStatus getStatus() {
        return tickManager.getStatus();
    }
}
