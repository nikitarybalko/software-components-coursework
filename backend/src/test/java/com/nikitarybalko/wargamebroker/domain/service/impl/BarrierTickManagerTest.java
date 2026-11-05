package com.nikitarybalko.wargamebroker.domain.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests: Barrier Tick Synchronization (BarrierTickManager)")
class BarrierTickManagerTest {

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
        tickManager = new BarrierTickManager(fogOfWarService, checkpointingService, simulatorRegistry);
        ReflectionTestUtils.setField(tickManager, "tickTimeoutMs", 1000L);
        ReflectionTestUtils.setField(tickManager, "snapshotInterval", 2);
    }

    @Test
    @DisplayName("Submitting in non-active status throws SimulationNotActiveException")
    void testSubmitWhenNotActiveThrowsException() {
        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);

        assertThrows(SimulationNotActiveException.class, () ->
                tickManager.submitUnits(Side.BLUE, 1, List.of(blue)));
    }

    @Test
    @DisplayName("Submitting invalid tick number throws InvalidTickException")
    void testSubmitInvalidTickThrowsException() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);

        assertThrows(InvalidTickException.class, () ->
                tickManager.submitUnits(Side.BLUE, 99, List.of(blue)));
    }

    @Test
    @DisplayName("Submitting mismatched side unit throws IllegalArgumentException")
    void testSubmitMismatchedSideUnitThrowsException() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        UnitState redUnitSentByBlue = new UnitState("r1", Side.RED, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);

        assertThrows(IllegalArgumentException.class, () ->
                tickManager.submitUnits(Side.BLUE, 1, List.of(redUnitSentByBlue)));
    }

    @Test
    @DisplayName("Barrier synchronization: tick completes only after both sides submit")
    void testBarrierSynchronizationFlow() {
        tickManager.setStatus(SimulationStatus.ACTIVE);

        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 50.0, UnitStatus.HEALTHY);
        UnitState red = new UnitState("r1", Side.RED, 30.0, 0.0, 50.0, UnitStatus.HEALTHY);

        when(fogOfWarService.calculateVisibleEnemies(anyList(), anyList()))
                .thenReturn(List.of(red))
                .thenReturn(List.of(blue));

        // 1. Blue submits
        tickManager.submitUnits(Side.BLUE, 1, List.of(blue));

        assertEquals(1, tickManager.getCurrentTick());
        assertFalse(tickManager.isTickComplete(1));
        verifyNoInteractions(fogOfWarService);

        // 2. Red submits
        tickManager.submitUnits(Side.RED, 1, List.of(red));

        // Tick 1 is complete, advanced to 2
        assertEquals(2, tickManager.getCurrentTick());
        assertTrue(tickManager.isTickComplete(1));
        verify(fogOfWarService, times(2)).calculateVisibleEnemies(anyList(), anyList());

        // Check FOW results
        List<UnitState> visibleForBlue = tickManager.getVisibleEnemies(Side.BLUE, 1);
        assertEquals(1, visibleForBlue.size());
        assertEquals("r1", visibleForBlue.get(0).getId());
    }

    @Test
    @DisplayName("Snapshot is saved every snapshotInterval ticks")
    void testSnapshotSavedAtInterval() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        when(fogOfWarService.calculateVisibleEnemies(anyList(), anyList())).thenReturn(List.of());

        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState red = new UnitState("r1", Side.RED, 100.0, 0.0, 10.0, UnitStatus.HEALTHY);

        tickManager.submitUnits(Side.BLUE, 1, List.of(blue));
        tickManager.submitUnits(Side.RED, 1, List.of(red));
        verify(checkpointingService, never()).checkAndSaveSnapshot(eq(1L), anyList());

        tickManager.submitUnits(Side.BLUE, 2, List.of(blue));
        tickManager.submitUnits(Side.RED, 2, List.of(red));
        verify(checkpointingService, times(1)).checkAndSaveSnapshot(eq(2L), anyList());
    }

    @Test
    @DisplayName("Timeout handling switches simulation to PAUSED and records event")
    void testTimeoutHandling() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);

        tickManager.submitUnits(Side.BLUE, 1, List.of(blue));

        ReflectionTestUtils.setField(tickManager, "lastSubmitTime", Instant.now().minusMillis(2000));

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

        assertEquals(1, tickManager.getCurrentTick());
        assertEquals(SimulationStatus.INITIALIZING, tickManager.getStatus());
        assertFalse(tickManager.isTickComplete(1));
        assertTrue(tickManager.getLatestVisibleEnemies(Side.BLUE).isEmpty());
    }

    @Test
    @DisplayName("Submitting from an unregistered side throws SimulatorNotRegisteredException")
    void testSubmitFromUnregisteredSideThrowsException() {
        BarrierTickManager manager = new BarrierTickManager(fogOfWarService, checkpointingService,
                new InMemorySimulatorRegistry());
        manager.setStatus(SimulationStatus.ACTIVE);
        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);

        assertThrows(SimulatorNotRegisteredException.class, () ->
                manager.submitUnits(Side.BLUE, 1, List.of(blue)));
    }

    @Test
    @DisplayName("Requesting visibility for an incomplete tick throws TickNotCompleteException")
    void testVisibilityForIncompleteTickThrowsException() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        tickManager.submitUnits(Side.BLUE, 1, List.of(new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY)));

        assertEquals(Set.of(Side.BLUE), tickManager.getSubmittedSides(1));
        assertThrows(TickNotCompleteException.class, () -> tickManager.getVisibleEnemies(Side.BLUE, 1));
        assertThrows(TickNotCompleteException.class, () -> tickManager.getVisibleEnemies(Side.RED, 5));
    }

    @Test
    @DisplayName("Requesting visibility for a tick without stored data throws TickDataUnavailableException")
    void testVisibilityForMissingPastTickThrowsException() {
        tickManager.restoreState(10, List.of());

        assertThrows(TickDataUnavailableException.class, () -> tickManager.getVisibleEnemies(Side.BLUE, 3));
    }

    @Test
    @DisplayName("Timeout pauses simulation when both sides are silent")
    void testTimeoutWhenBothSidesSilent() {
        tickManager.setStatus(SimulationStatus.ACTIVE);
        ReflectionTestUtils.setField(tickManager, "lastSubmitTime", Instant.now().minusMillis(2000));

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
        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState damagedRed = new UnitState("r1", Side.RED, 100.0, 0.0, 10.0, UnitStatus.DAMAGED);
        UnitState destroyedRed = new UnitState("r1", Side.RED, 100.0, 0.0, 10.0, UnitStatus.DESTROYED);

        submitBothSides(1, blue, damagedRed);
        submitBothSides(2, blue, destroyedRed);
        submitBothSides(3, blue, destroyedRed);

        verify(checkpointingService, times(1)).recordEvent(eq(EventType.UNIT_DAMAGED), eq(Side.RED), anyString());
        verify(checkpointingService, times(1)).recordEvent(eq(EventType.UNIT_DESTROYED), eq(Side.RED), anyString());
        verify(checkpointingService, never()).recordEvent(any(), eq(Side.BLUE), anyString());
    }

    private void submitBothSides(long tick, UnitState blue, UnitState red) {
        tickManager.submitUnits(Side.BLUE, tick, List.of(blue));
        tickManager.submitUnits(Side.RED, tick, List.of(red));
    }
}
