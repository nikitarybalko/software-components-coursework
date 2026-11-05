package com.nikitarybalko.wargamebroker.domain.service.impl;

import com.nikitarybalko.wargamebroker.domain.exception.InvalidLifecycleTransitionException;
import com.nikitarybalko.wargamebroker.domain.exception.SnapshotNotFoundException;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationSessionServiceImpl implements SimulationSessionService {

    private static final String DEFAULT_PAUSE_REASON = "User requested pause";

    private final TickManager tickManager;
    private final StateCheckpointingService checkpointingService;
    private final SimulatorRegistry simulatorRegistry;

    @Override
    public void registerSimulator(Side side) {
        if (simulatorRegistry.register(side)) {
            checkpointingService.recordEvent(EventType.SIMULATOR_REGISTERED, side, side + " simulator registered");
            log.info("Simulator registered for side {}", side);
        }
    }

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
        checkpointingService.clearAll();
        checkpointingService.recordEvent(EventType.SIMULATION_RESET, null, "Simulation reset to initial state");
        log.info("Simulation reset, persisted snapshots and events cleared");
    }

    @Override
    public void pauseSimulation(String reason) {
        requireStatus(SimulationStatus.ACTIVE, "pause");
        tickManager.setStatus(SimulationStatus.PAUSED);
        String message = Objects.nonNull(reason) ? reason : DEFAULT_PAUSE_REASON;
        checkpointingService.recordEvent(EventType.SIMULATION_PAUSED, null, message);
        log.info("Simulation paused: {}", message);
    }

    @Override
    public void resumeSimulation() {
        requireStatus(SimulationStatus.PAUSED, "resume");
        tickManager.setStatus(SimulationStatus.ACTIVE);
        checkpointingService.recordEvent(EventType.SIMULATION_RESUMED, null, "Simulation session resumed");
        log.info("Simulation resumed");
    }

    @Override
    public void restoreFromSnapshot(UUID snapshotId) {
        Snapshot snapshot = checkpointingService.loadSnapshot(snapshotId)
                .orElseThrow(() -> new SnapshotNotFoundException(snapshotId));

        tickManager.restoreState(snapshot.getTickNumber(), snapshot.getUnits());
        checkpointingService.recordEvent(EventType.SNAPSHOT_RESTORED, null,
                "Restored simulation state from snapshot " + snapshotId + " at tick " + snapshot.getTickNumber());
        log.info("Successfully restored simulation state from snapshot {}", snapshotId);
    }

    @Override
    public SimulationStatus getStatus() {
        return tickManager.getStatus();
    }

    private void requireStatus(SimulationStatus expected, String action) {
        SimulationStatus current = tickManager.getStatus();
        if (current != expected) {
            throw new InvalidLifecycleTransitionException(action, current);
        }
    }
}
