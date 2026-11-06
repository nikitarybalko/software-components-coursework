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
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.support.TestSimulationSettings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.EXPIRED_SUBMISSION_AGE_MS;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.FIRST_TICK;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.SECOND_TICK;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.SNAPSHOT_INTERVAL;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.BLUE_UNIT;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests: Barrier Tick Synchronization (BarrierTickManager)")
class BarrierTickManagerTest {

    private static final String LAST_SUBMIT_TIME_FIELD = "lastSubmitTime";
    private static final double ANY_VIEW_RADIUS = 10.0;
    private static final long NON_CURRENT_TICK = 99;
    private static final long FUTURE_TICK = 5;
    private static final long RESTORED_SNAPSHOT_TICK = 10;
    private static final long PRUNED_TICK = 3;

    @Mock
    private FogOfWarService fogOfWarService;

    @Mock
    private StateCheckpointingService checkpointingService;

    private InMemorySimulatorRegistry simulatorRegistry;

    private BarrierTickManager tickManager;

    @BeforeEach
    void setUp() {
        simulatorRegistry = new InMemorySimulatorRegistry();
        simulatorRegistry.register(Side.BLUE);
        simulatorRegistry.register(Side.RED);
        tickManager = new BarrierTickManager(fogOfWarService, checkpointingService, simulatorRegistry,
                TestSimulationSettings.properties());
    }

    @Test
    @DisplayName("Submitting in non-active status throws SimulationNotActiveException")
    void testSubmitWhenNotActiveThrowsException() {
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);

        assertThrows(SimulationNotActiveException.class, () ->
                tickManager.submitUnits(Side.BLUE, FIRST_TICK, List.of(blue)));
    }

    @Test
    @DisplayName("Submitting invalid tick number throws InvalidTickException")
    void testSubmitInvalidTickThrowsException() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);

        assertThrows(InvalidTickException.class, () ->
                tickManager.submitUnits(Side.BLUE, NON_CURRENT_TICK, List.of(blue)));
    }

    @Test
    @DisplayName("Submitting mismatched side unit throws IllegalArgumentException")
    void testSubmitMismatchedSideUnitThrowsException() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        UnitState redUnitSentByBlue = new UnitState(RED_UNIT, Side.RED, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);

        assertThrows(IllegalArgumentException.class, () ->
                tickManager.submitUnits(Side.BLUE, FIRST_TICK, List.of(redUnitSentByBlue)));
    }

    @Test
    @DisplayName("Barrier synchronization: tick completes only after both sides submit")
    void testBarrierSynchronizationFlow() {
        tickManager.setStatus(SimulationStatus.ACTIVE);

        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 50.0, UnitStatus.HEALTHY);
        UnitState red = new UnitState(RED_UNIT, Side.RED, 30.0, 0.0, 50.0, UnitStatus.HEALTHY);

        when(fogOfWarService.calculateVisibleEnemies(anyList(), anyList()))
                .thenReturn(List.of(red))
                .thenReturn(List.of(blue));

        tickManager.submitUnits(Side.BLUE, FIRST_TICK, List.of(blue));

        assertEquals(FIRST_TICK, tickManager.getCurrentTick());
        assertFalse(tickManager.isTickComplete(FIRST_TICK));
        verifyNoInteractions(fogOfWarService);

        tickManager.submitUnits(Side.RED, FIRST_TICK, List.of(red));

        assertEquals(SECOND_TICK, tickManager.getCurrentTick());
        assertTrue(tickManager.isTickComplete(FIRST_TICK));
        verify(fogOfWarService, times(2)).calculateVisibleEnemies(anyList(), anyList());

        List<UnitState> visibleForBlue = tickManager.getVisibleEnemies(Side.BLUE, FIRST_TICK);
        assertEquals(1, visibleForBlue.size());
        assertEquals(RED_UNIT, visibleForBlue.get(0).getId());
    }

    @Test
    @DisplayName("Snapshot is saved every snapshotInterval ticks")
    void testSnapshotSavedAtInterval() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        when(fogOfWarService.calculateVisibleEnemies(anyList(), anyList())).thenReturn(List.of());

        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);
        UnitState red = new UnitState(RED_UNIT, Side.RED, 100.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);

        tickManager.submitUnits(Side.BLUE, FIRST_TICK, List.of(blue));
        tickManager.submitUnits(Side.RED, FIRST_TICK, List.of(red));
        verify(checkpointingService, never()).checkAndSaveSnapshot(eq(FIRST_TICK), anyList());

        tickManager.submitUnits(Side.BLUE, SECOND_TICK, List.of(blue));
        tickManager.submitUnits(Side.RED, SECOND_TICK, List.of(red));
        verify(checkpointingService, times(1)).checkAndSaveSnapshot(eq((long) SNAPSHOT_INTERVAL), anyList());
    }

    @Test
    @DisplayName("Timeout handling switches simulation to PAUSED and records event")
    void testTimeoutHandling() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);

        tickManager.submitUnits(Side.BLUE, FIRST_TICK, List.of(blue));

        ReflectionTestUtils.setField(tickManager, LAST_SUBMIT_TIME_FIELD, Instant.now().minusMillis(EXPIRED_SUBMISSION_AGE_MS));

        tickManager.checkTimeouts();

        assertEquals(SimulationStatus.PAUSED, tickManager.getStatus());
        verify(checkpointingService).recordEvent(eq(EventType.TIMEOUT_DISCONNECT), eq(Side.RED), anyString());
        verify(checkpointingService).recordEvent(eq(EventType.SIMULATION_PAUSED), isNull(), anyString());
    }

    @Test
    @DisplayName("Reset clears state and sets current tick back to 1")
    void testResetClearsState() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        tickManager.reset();

        assertEquals(FIRST_TICK, tickManager.getCurrentTick());
        assertEquals(SimulationStatus.INITIALIZING, tickManager.getStatus());
        assertFalse(tickManager.isTickComplete(FIRST_TICK));
        assertTrue(tickManager.getLatestVisibleEnemies(Side.BLUE).isEmpty());
    }

    @Test
    @DisplayName("Submitting from an unregistered side throws SimulatorNotRegisteredException")
    void testSubmitFromUnregisteredSideThrowsException() {
        BarrierTickManager manager = new BarrierTickManager(fogOfWarService, checkpointingService,
                new InMemorySimulatorRegistry(), TestSimulationSettings.properties());
        manager.setStatus(SimulationStatus.ACTIVE);
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);

        assertThrows(SimulatorNotRegisteredException.class, () ->
                manager.submitUnits(Side.BLUE, FIRST_TICK, List.of(blue)));
    }

    @Test
    @DisplayName("Requesting visibility for an incomplete tick throws TickNotCompleteException")
    void testVisibilityForIncompleteTickThrowsException() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        tickManager.submitUnits(Side.BLUE, FIRST_TICK, List.of(new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY)));

        assertEquals(Set.of(Side.BLUE), tickManager.getSubmittedSides(FIRST_TICK));
        assertThrows(TickNotCompleteException.class, () -> tickManager.getVisibleEnemies(Side.BLUE, FIRST_TICK));
        assertThrows(TickNotCompleteException.class, () -> tickManager.getVisibleEnemies(Side.RED, FUTURE_TICK));
    }

    @Test
    @DisplayName("Requesting visibility for a tick without stored data throws TickDataUnavailableException")
    void testVisibilityForMissingPastTickThrowsException() {
        tickManager.restoreState(RESTORED_SNAPSHOT_TICK, List.of());

        assertThrows(TickDataUnavailableException.class, () -> tickManager.getVisibleEnemies(Side.BLUE, PRUNED_TICK));
    }

    @Test
    @DisplayName("Timeout pauses simulation when both sides are silent")
    void testTimeoutWhenBothSidesSilent() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        ReflectionTestUtils.setField(tickManager, LAST_SUBMIT_TIME_FIELD, Instant.now().minusMillis(EXPIRED_SUBMISSION_AGE_MS));

        tickManager.checkTimeouts();

        assertEquals(SimulationStatus.PAUSED, tickManager.getStatus());
        verify(checkpointingService).recordEvent(eq(EventType.TIMEOUT_DISCONNECT), eq(Side.BLUE), anyString());
        verify(checkpointingService).recordEvent(eq(EventType.TIMEOUT_DISCONNECT), eq(Side.RED), anyString());
    }

    @Test
    @DisplayName("Timeout check does nothing while the timeout has not elapsed")
    void testNoTimeoutBeforeDeadline() {
        tickManager.setStatus(SimulationStatus.ACTIVE);

        tickManager.checkTimeouts();

        assertEquals(SimulationStatus.ACTIVE, tickManager.getStatus());
        verifyNoInteractions(checkpointingService);
    }

    @Test
    @DisplayName("Unit casualty events are recorded once per status transition")
    void testCasualtyEventsRecordedOncePerTransition() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        when(fogOfWarService.calculateVisibleEnemies(anyList(), anyList())).thenReturn(List.of());
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.HEALTHY);
        UnitState damagedRed = new UnitState(RED_UNIT, Side.RED, 100.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.DAMAGED);
        UnitState destroyedRed = new UnitState(RED_UNIT, Side.RED, 100.0, 0.0, ANY_VIEW_RADIUS, UnitStatus.DESTROYED);

        submitBothSides(FIRST_TICK, blue, damagedRed);
        submitBothSides(SECOND_TICK, blue, destroyedRed);
        submitBothSides(SECOND_TICK + 1, blue, destroyedRed);

        verify(checkpointingService, times(1)).recordEvent(eq(EventType.UNIT_DAMAGED), eq(Side.RED), anyString());
        verify(checkpointingService, times(1)).recordEvent(eq(EventType.UNIT_DESTROYED), eq(Side.RED), anyString());
        verify(checkpointingService, never()).recordEvent(any(), eq(Side.BLUE), anyString());
    }

    @Test
    @DisplayName("State epoch increments on reset and restore, after restored units are in place")
    void testEpochIncrementsOnResetAndRestore() {
        long initialEpoch = tickManager.getEpoch();
        UnitState restoredBlue = new UnitState(BLUE_UNIT, Side.BLUE, 42.0, 7.0, ANY_VIEW_RADIUS, UnitStatus.DAMAGED);

        tickManager.reset();
        assertEquals(initialEpoch + 1, tickManager.getEpoch());

        tickManager.restoreState(RESTORED_SNAPSHOT_TICK, List.of(restoredBlue));
        assertEquals(initialEpoch + 2, tickManager.getEpoch());
        assertEquals(RESTORED_SNAPSHOT_TICK + 1, tickManager.getCurrentTick());
        assertEquals(List.of(restoredBlue), tickManager.getLatestUnits(Side.BLUE));
        assertTrue(tickManager.getLatestUnits(Side.RED).isEmpty());
    }

    private void submitBothSides(long tick, UnitState blue, UnitState red) {
        tickManager.submitUnits(Side.BLUE, tick, List.of(blue));
        tickManager.submitUnits(Side.RED, tick, List.of(red));
    }
}
