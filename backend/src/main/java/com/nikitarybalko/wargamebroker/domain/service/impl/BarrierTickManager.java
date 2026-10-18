package com.nikitarybalko.wargamebroker.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nikitarybalko.wargamebroker.domain.exception.InvalidTickException;
import com.nikitarybalko.wargamebroker.domain.exception.SimulationNotActiveException;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.FogOfWarService;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Service
public class BarrierTickManager implements TickManager {

    private final FogOfWarService fogOfWarService;
    private final StateCheckpointingService checkpointingService;

    @Value("${wargame.simulation.tick-timeout-ms:10000}")
    private long tickTimeoutMs;

    @Value("${wargame.simulation.snapshot-interval:5}")
    private int snapshotInterval;

    private static final int MAX_BUFFER_HISTORY = 50;

    private volatile long currentTick = 1;
    private volatile SimulationStatus status = SimulationStatus.INITIALIZING;
    private volatile Instant lastSubmitTime = Instant.now();

    private final Map<Long, List<UnitState>> blueBuffers = new ConcurrentHashMap<>();
    private final Map<Long, List<UnitState>> redBuffers = new ConcurrentHashMap<>();
    private final Map<Long, List<UnitState>> blueVisibleCache = new ConcurrentHashMap<>();
    private final Map<Long, List<UnitState>> redVisibleCache = new ConcurrentHashMap<>();
    private final Map<Side, List<UnitState>> latestUnits = new ConcurrentHashMap<>();

    public BarrierTickManager(FogOfWarService fogOfWarService,
                              StateCheckpointingService checkpointingService) {
        this.fogOfWarService = fogOfWarService;
        this.checkpointingService = checkpointingService;
    }

    @Override
    public long getCurrentTick() {
        return currentTick;
    }

    @Override
    public SimulationStatus getStatus() {
        return status;
    }

    @Override
    public synchronized void setStatus(SimulationStatus newStatus) {
        log.info("Simulation status changed: {} -> {}", this.status, newStatus);
        this.status = newStatus;
        this.lastSubmitTime = Instant.now();
    }

    @Override
    public synchronized void submitUnits(Side side, long tickNumber, List<UnitState> units) {
        validateSubmission(side, tickNumber, units);

        List<UnitState> safeUnits = units != null ? List.copyOf(units) : Collections.emptyList();
        storeBuffer(side, tickNumber, safeUnits);

        if (isBothSidesSubmitted(tickNumber)) {
            processTick(tickNumber);
        }
    }

    @Override
    public boolean isTickComplete(long tickNumber) {
        return blueVisibleCache.containsKey(tickNumber) && redVisibleCache.containsKey(tickNumber);
    }

    @Override
    public List<UnitState> getVisibleEnemies(Side side, long tickNumber) {
        Map<Long, List<UnitState>> cache = (side == Side.BLUE) ? blueVisibleCache : redVisibleCache;
        return cache.getOrDefault(tickNumber, Collections.emptyList());
    }

    @Override
    public List<UnitState> getLatestUnits(Side side) {
        return latestUnits.getOrDefault(side, Collections.emptyList());
    }

    @Override
    public synchronized void checkTimeouts() {
        if (status != SimulationStatus.ACTIVE) {
            return;
        }

        boolean blueReady = blueBuffers.containsKey(currentTick);
        boolean redReady = redBuffers.containsKey(currentTick);

        if (blueReady ^ redReady) {
            handleSingleSideTimeout(blueReady ? Side.RED : Side.BLUE);
        }
    }

    @Override
    public synchronized void reset() {
        currentTick = 1;
        status = SimulationStatus.INITIALIZING;
        lastSubmitTime = Instant.now();
        blueBuffers.clear();
        redBuffers.clear();
        blueVisibleCache.clear();
        redVisibleCache.clear();
        latestUnits.clear();
        log.info("BarrierTickManager reset successfully");
    }

    @Override
    public synchronized void restoreState(long tickNumber, List<UnitState> units) {
        reset();
        currentTick = tickNumber + 1;

        latestUnits.put(Side.BLUE, filterUnitsBySide(units, Side.BLUE));
        latestUnits.put(Side.RED, filterUnitsBySide(units, Side.RED));

        setStatus(SimulationStatus.PAUSED);
        log.info("Restored simulation state at tick {}, next tick will be {}", tickNumber, currentTick);
    }

    private void validateSubmission(Side side, long tickNumber, List<UnitState> units) {
        if (status != SimulationStatus.ACTIVE) {
            throw new SimulationNotActiveException(status);
        }
        if (tickNumber != currentTick) {
            throw new InvalidTickException(currentTick, tickNumber);
        }
        if (units != null && units.stream().anyMatch(unit -> unit.getSide() != side)) {
            throw new IllegalArgumentException("Units list contains unit belonging to opponent side");
        }
    }

    private void storeBuffer(Side side, long tickNumber, List<UnitState> safeUnits) {
        if (side == Side.BLUE) {
            blueBuffers.put(tickNumber, safeUnits);
        } else {
            redBuffers.put(tickNumber, safeUnits);
        }
        latestUnits.put(side, safeUnits);
        lastSubmitTime = Instant.now();
    }

    private boolean isBothSidesSubmitted(long tickNumber) {
        return blueBuffers.containsKey(tickNumber) && redBuffers.containsKey(tickNumber);
    }

    private void processTick(long tickNumber) {
        List<UnitState> blues = blueBuffers.get(tickNumber);
        List<UnitState> reds = redBuffers.get(tickNumber);

        calculateAndCacheVisibility(tickNumber, blues, reds);
        recordDestroyedCasualties(blues, reds);
        createSnapshotIfRequired(tickNumber, blues, reds);

        currentTick++;
        pruneOldBuffers();
    }

    private void calculateAndCacheVisibility(long tickNumber, List<UnitState> blues, List<UnitState> reds) {
        List<UnitState> blueSpotsRed = fogOfWarService.calculateVisibleEnemies(blues, reds);
        List<UnitState> redSpotsBlue = fogOfWarService.calculateVisibleEnemies(reds, blues);

        blueVisibleCache.put(tickNumber, blueSpotsRed);
        redVisibleCache.put(tickNumber, redSpotsBlue);

        log.info("Completed tick {}. Blue spotted {} reds, Red spotted {} blues",
                tickNumber, blueSpotsRed.size(), redSpotsBlue.size());
    }

    private void recordDestroyedCasualties(List<UnitState> blues, List<UnitState> reds) {
        recordDestroyedUnits(reds, Side.RED);
        recordDestroyedUnits(blues, Side.BLUE);
    }

    private void recordDestroyedUnits(List<UnitState> units, Side side) {
        units.stream()
                .filter(unit -> unit.getStatus() == UnitStatus.DESTROYED)
                .forEach(unit -> checkpointingService.recordEvent(
                        EventType.UNIT_DESTROYED,
                        side,
                        side + " unit " + unit.getId() + " is destroyed"));
    }

    private void createSnapshotIfRequired(long tickNumber, List<UnitState> blues, List<UnitState> reds) {
        if (tickNumber % snapshotInterval == 0) {
            List<UnitState> allUnits = Stream.concat(blues.stream(), reds.stream()).toList();
            checkpointingService.checkAndSaveSnapshot(tickNumber, allUnits);
        }
    }

    private void handleSingleSideTimeout(Side missingSide) {
        long elapsed = Duration.between(lastSubmitTime, Instant.now()).toMillis();
        if (elapsed > tickTimeoutMs) {
            log.warn("Tick {} timed out waiting for side {}. Pausing simulation.", currentTick, missingSide);
            setStatus(SimulationStatus.PAUSED);
            checkpointingService.recordEvent(EventType.TIMEOUT_DISCONNECT, missingSide,
                    "Timeout exceeded (" + elapsed + "ms) waiting for side " + missingSide);
            checkpointingService.recordEvent(EventType.SIMULATION_PAUSED, null,
                    "Simulation automatically paused due to timeout at tick " + currentTick);
        }
    }

    private List<UnitState> filterUnitsBySide(List<UnitState> units, Side side) {
        if (units == null) return Collections.emptyList();
        return units.stream()
                .filter(u -> u.getSide() == side)
                .toList();
    }

    private void pruneOldBuffers() {
        long threshold = currentTick - MAX_BUFFER_HISTORY;
        if (threshold <= 0) return;

        blueBuffers.keySet().removeIf(t -> t < threshold);
        redBuffers.keySet().removeIf(t -> t < threshold);
        blueVisibleCache.keySet().removeIf(t -> t < threshold);
        redVisibleCache.keySet().removeIf(t -> t < threshold);
    }
}
