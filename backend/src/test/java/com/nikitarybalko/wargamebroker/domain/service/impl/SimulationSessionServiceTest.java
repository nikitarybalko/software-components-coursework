package com.nikitarybalko.wargamebroker.domain.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.nikitarybalko.wargamebroker.domain.exception.SimulationException;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests: Simulation Session Lifecycle (SimulationSessionService)")
class SimulationSessionServiceTest {

    @Mock
    private TickManager tickManager;

    @Mock
    private StateCheckpointingService checkpointingService;

    private SimulationSessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SimulationSessionServiceImpl(tickManager, checkpointingService);
    }

    @Test
    @DisplayName("initSimulation resets tick manager and activates simulation")
    void testInitSimulation() {
        sessionService.initSimulation();

        verify(tickManager).reset();
        verify(tickManager).setStatus(SimulationStatus.ACTIVE);
        verify(checkpointingService).recordEvent(eq(EventType.SIMULATION_INITIALIZED), isNull(), anyString());
    }

    @Test
    @DisplayName("pauseSimulation and resumeSimulation change status and record events")
    void testPauseAndResumeSimulation() {
        sessionService.pauseSimulation("Test reason");
        verify(tickManager).setStatus(SimulationStatus.PAUSED);
        verify(checkpointingService).recordEvent(eq(EventType.SIMULATION_PAUSED), isNull(), eq("Test reason"));

        sessionService.resumeSimulation();
        verify(tickManager).setStatus(SimulationStatus.ACTIVE);
        verify(checkpointingService).recordEvent(eq(EventType.SIMULATION_RESUMED), isNull(), anyString());
    }

    @Test
    @DisplayName("restoreFromSnapshot restores state and records event")
    void testRestoreFromSnapshot() {
        UUID snapshotId = UUID.randomUUID();
        UnitState u = new UnitState("u1", Side.BLUE, 10.0, 10.0, 20.0, UnitStatus.HEALTHY);
        Snapshot snapshot = new Snapshot(snapshotId, 5L, Instant.now(), List.of(u));

        when(checkpointingService.loadSnapshot(snapshotId)).thenReturn(Optional.of(snapshot));

        sessionService.restoreFromSnapshot(snapshotId);

        verify(tickManager).restoreState(eq(5L), eq(List.of(u)));
        verify(checkpointingService).recordEvent(eq(EventType.SNAPSHOT_RESTORED), isNull(), anyString());
    }

    @Test
    @DisplayName("restoreFromSnapshot for non-existent snapshot throws SimulationException")
    void testRestoreNotFoundThrowsException() {
        UUID snapshotId = UUID.randomUUID();
        when(checkpointingService.loadSnapshot(snapshotId)).thenReturn(Optional.empty());

        assertThrows(SimulationException.class, () ->
                sessionService.restoreFromSnapshot(snapshotId));
    }
}
