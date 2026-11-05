package com.nikitarybalko.wargamebroker.domain.service.impl;

import com.nikitarybalko.wargamebroker.domain.exception.InvalidTickException;
import com.nikitarybalko.wargamebroker.domain.exception.SimulationNotActiveException;
import com.nikitarybalko.wargamebroker.domain.exception.SimulatorNotRegisteredException;
import com.nikitarybalko.wargamebroker.domain.exception.TickDataUnavailableException;
import com.nikitarybalko.wargamebroker.domain.exception.TickNotCompleteException;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.FogOfWarService;
import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class BarrierTickManager implements TickManager {

    private static final int MAX_BUFFER_HISTORY = 50;

    private final FogOfWarService fogOfWarService;
    private final StateCheckpointingService checkpointingService;
    private final SimulatorRegistry simulatorRegistry;

    @Value("${wargame.simulation.tick-timeout-ms:10000}")
    private long tickTimeoutMs;

    @Value("${wargame.simulation.snapshot-interval:5}")
    private int snapshotInterval;

    private volatile long currentTick = 1;
    private volatile SimulationStatus status = SimulationStatus.INITIALIZING;
    private volatile Instant lastSubmitTime = Instant.now();

    private final Map<Side, Map<Long, List<UnitState>>> submissions = createPerSideStorage();
    private final Map<Side, Map<Long, List<UnitState>>> visibleEnemies = createPerSideStorage();
    private final Map<Side, List<UnitState>> latestUnits = new ConcurrentHashMap<>();
    private final Map<UnitKey, UnitStatus> knownUnitStatuses = new ConcurrentHashMap<>();

    public BarrierTickManager(FogOfWarService fogOfWarService,
                              StateCheckpointingService checkpointingService,
                              SimulatorRegistry simulatorRegistry) {
        this.fogOfWarService = fogOfWarService;
        this.checkpointingService = checkpointingService;
        this.simulatorRegistry = simulatorRegistry;
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

        List<UnitState> safeUnits = Objects.isNull(units) ? Collections.emptyList() : List.copyOf(units);
        storeSubmission(side, tickNumber, safeUnits);

        if (isBothSidesSubmitted(tickNumber)) {
            processTick(tickNumber);
        }
    }

    @Override
    public boolean isTickComplete(long tickNumber) {
        return Arrays.stream(Side.values())
                .allMatch(side -> visibleEnemies.get(side).containsKey(tickNumber));
    }

    @Override
    public Set<Side> getSubmittedSides(long tickNumber) {
        return Arrays.stream(Side.values())
                .filter(side -> submissions.get(side).containsKey(tickNumber))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Side.class)));
    }

    @Override
    public synchronized List<UnitState> getVisibleEnemies(Side side, long tickNumber) {
        List<UnitState> visible = visibleEnemies.get(side).get(tickNumber);
        if (Objects.nonNull(visible)) {
            return visible;
        }
        if (tickNumber >= currentTick) {
            throw new TickNotCompleteException(tickNumber);
        }
        throw new TickDataUnavailableException(tickNumber);
    }

    @Override
    public List<UnitState> getLatestVisibleEnemies(Side side) {
        return visibleEnemies.get(side).getOrDefault(currentTick - 1, Collections.emptyList());
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

        long elapsed = Duration.between(lastSubmitTime, Instant.now()).toMillis();
        if (elapsed > tickTimeoutMs) {
            pauseDueToTimeout(findSilentSides(), elapsed);
        }
    }

    @Override
    public synchronized void reset() {
        currentTick = 1;
        status = SimulationStatus.INITIALIZING;
        lastSubmitTime = Instant.now();
        Stream.of(submissions, visibleEnemies)
                .flatMap(storage -> storage.values().stream())
                .forEach(Map::clear);
        latestUnits.clear();
        knownUnitStatuses.clear();
        log.info("BarrierTickManager reset successfully");
    }

    @Override
    public synchronized void restoreState(long tickNumber, List<UnitState> units) {
        reset();
        currentTick = tickNumber + 1;

        List<UnitState> restoredUnits = Objects.isNull(units) ? Collections.emptyList() : units;
        Arrays.stream(Side.values())
                .forEach(side -> latestUnits.put(side, filterUnitsBySide(restoredUnits, side)));
        rememberUnitStatuses(restoredUnits);

        setStatus(SimulationStatus.PAUSED);
        log.info("Restored simulation state at tick {}, next tick will be {}", tickNumber, currentTick);
    }

    private static Map<Side, Map<Long, List<UnitState>>> createPerSideStorage() {
        Map<Side, Map<Long, List<UnitState>>> storage = new EnumMap<>(Side.class);
        Arrays.stream(Side.values()).forEach(side -> storage.put(side, new ConcurrentHashMap<>()));
        return Collections.unmodifiableMap(storage);
    }

    private void validateSubmission(Side side, long tickNumber, List<UnitState> units) {
        if (status != SimulationStatus.ACTIVE) {
            throw new SimulationNotActiveException(status);
        }
        if (!simulatorRegistry.isRegistered(side)) {
            throw new SimulatorNotRegisteredException(side);
        }
        if (tickNumber != currentTick) {
            throw new InvalidTickException(currentTick, tickNumber);
        }
        if (Objects.nonNull(units) && units.stream().anyMatch(unit -> unit.getSide() != side)) {
            throw new IllegalArgumentException("Units list contains unit belonging to opponent side");
        }
    }

    private void storeSubmission(Side side, long tickNumber, List<UnitState> safeUnits) {
        submissions.get(side).put(tickNumber, safeUnits);
        latestUnits.put(side, safeUnits);
        lastSubmitTime = Instant.now();
    }

    private boolean isBothSidesSubmitted(long tickNumber) {
        return getSubmittedSides(tickNumber).size() == Side.values().length;
    }

    private void processTick(long tickNumber) {
        List<UnitState> blues = submissions.get(Side.BLUE).get(tickNumber);
        List<UnitState> reds = submissions.get(Side.RED).get(tickNumber);

        calculateAndCacheVisibility(tickNumber, blues, reds);
        recordStatusTransitions(Stream.concat(blues.stream(), reds.stream()).toList());
        createSnapshotIfRequired(tickNumber, blues, reds);

        currentTick++;
        pruneOldBuffers();
    }

    private void calculateAndCacheVisibility(long tickNumber, List<UnitState> blues, List<UnitState> reds) {
        List<UnitState> blueSpotsRed = fogOfWarService.calculateVisibleEnemies(blues, reds);
        List<UnitState> redSpotsBlue = fogOfWarService.calculateVisibleEnemies(reds, blues);

        visibleEnemies.get(Side.BLUE).put(tickNumber, blueSpotsRed);
        visibleEnemies.get(Side.RED).put(tickNumber, redSpotsBlue);

        log.info("Completed tick {}. Blue spotted {} reds, Red spotted {} blues",
                tickNumber, blueSpotsRed.size(), redSpotsBlue.size());
    }

    private void recordStatusTransitions(List<UnitState> units) {
        units.stream()
                .filter(this::isNewCasualty)
                .forEach(this::recordCasualty);
        rememberUnitStatuses(units);
    }

    private boolean isNewCasualty(UnitState unit) {
        UnitStatus previous = knownUnitStatuses.getOrDefault(UnitKey.of(unit), UnitStatus.HEALTHY);
        return unit.getStatus() != previous && unit.getStatus() != UnitStatus.HEALTHY;
    }

    private void recordCasualty(UnitState unit) {
        EventType type = unit.getStatus() == UnitStatus.DESTROYED ? EventType.UNIT_DESTROYED : EventType.UNIT_DAMAGED;
        checkpointingService.recordEvent(type, unit.getSide(),
                unit.getSide() + " unit " + unit.getId() + " is " + unit.getStatus().name().toLowerCase());
    }

    private void rememberUnitStatuses(List<UnitState> units) {
        units.forEach(unit -> knownUnitStatuses.put(UnitKey.of(unit), unit.getStatus()));
    }

    private void createSnapshotIfRequired(long tickNumber, List<UnitState> blues, List<UnitState> reds) {
        if (tickNumber % snapshotInterval == 0) {
            List<UnitState> allUnits = Stream.concat(blues.stream(), reds.stream()).toList();
            checkpointingService.checkAndSaveSnapshot(tickNumber, allUnits);
        }
    }

    private List<Side> findSilentSides() {
        return Arrays.stream(Side.values())
                .filter(side -> !submissions.get(side).containsKey(currentTick))
                .toList();
    }

    private void pauseDueToTimeout(List<Side> silentSides, long elapsed) {
        log.warn("Tick {} timed out waiting for {}. Pausing simulation.", currentTick, silentSides);
        setStatus(SimulationStatus.PAUSED);
        silentSides.forEach(side -> checkpointingService.recordEvent(EventType.TIMEOUT_DISCONNECT, side,
                "Timeout exceeded (" + elapsed + "ms) waiting for side " + side + " at tick " + currentTick));
        checkpointingService.recordEvent(EventType.SIMULATION_PAUSED, null,
                "Simulation automatically paused due to timeout at tick " + currentTick);
    }

    private List<UnitState> filterUnitsBySide(List<UnitState> units, Side side) {
        return units.stream()
                .filter(unit -> unit.getSide() == side)
                .toList();
    }

    private void pruneOldBuffers() {
        long threshold = currentTick - MAX_BUFFER_HISTORY;
        if (threshold <= 0) {
            return;
        }
        Stream.of(submissions, visibleEnemies)
                .flatMap(storage -> storage.values().stream())
                .forEach(buffer -> buffer.keySet().removeIf(tick -> tick < threshold));
    }

    private record UnitKey(Side side, String id) {
        static UnitKey of(UnitState unit) {
            return new UnitKey(unit.getSide(), unit.getId());
        }
    }
}
