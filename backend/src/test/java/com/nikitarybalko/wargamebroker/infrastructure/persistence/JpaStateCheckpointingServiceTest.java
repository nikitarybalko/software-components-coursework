package com.nikitarybalko.wargamebroker.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import com.nikitarybalko.wargamebroker.domain.model.EventLog;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.repository.EventLogRepository;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.repository.SnapshotRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Integration tests: Persistence & Checkpointing (JpaStateCheckpointingService)")
class JpaStateCheckpointingServiceTest {

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private EventLogRepository eventLogRepository;

    private JpaStateCheckpointingService checkpointingService;

    @BeforeEach
    void setUp() {
        checkpointingService = new JpaStateCheckpointingService(snapshotRepository, eventLogRepository);
        checkpointingService.clearAll();
    }

    @Test
    @DisplayName("checkAndSaveSnapshot persists all units and records SNAPSHOT_SAVED event")
    void testCheckAndSaveSnapshotPersistsUnits() {
        UnitState blue = new UnitState("b1", Side.BLUE, 10.5, 20.5, 50.0, UnitStatus.HEALTHY);
        UnitState red = new UnitState("r1", Side.RED, 15.0, 25.0, 40.0, UnitStatus.DAMAGED);

        checkpointingService.checkAndSaveSnapshot(5L, List.of(blue, red));

        List<Snapshot> snapshots = checkpointingService.getRecentSnapshots();
        assertEquals(1, snapshots.size());

        Snapshot savedSnapshot = snapshots.get(0);
        assertEquals(5L, savedSnapshot.getTickNumber());
        assertEquals(2, savedSnapshot.getUnits().size());

        Optional<Snapshot> loaded = checkpointingService.loadSnapshot(savedSnapshot.getId());
        assertTrue(loaded.isPresent());
        assertEquals(2, loaded.get().getUnits().size());

        List<EventLog> events = checkpointingService.getRecentEvents();
        assertFalse(events.isEmpty());
        assertEquals(EventType.SNAPSHOT_SAVED, events.get(0).getType());
    }

    @Test
    @DisplayName("recordEvent saves events and retrieves them in reverse chronological order")
    void testRecordEventAndOrdering() {
        checkpointingService.recordEvent(EventType.SIMULATION_INITIALIZED, null, "Started");
        checkpointingService.recordEvent(EventType.UNIT_DESTROYED, Side.RED, "Red destroyed");

        List<EventLog> events = checkpointingService.getRecentEvents();
        assertEquals(2, events.size());
        assertEquals(EventType.UNIT_DESTROYED, events.get(0).getType());
        assertEquals(EventType.SIMULATION_INITIALIZED, events.get(1).getType());
    }

    @Test
    @DisplayName("clearAll deletes all snapshots and event logs from database")
    void testClearAllDeletesData() {
        checkpointingService.recordEvent(EventType.SIMULATION_RESET, null, "Reset");
        checkpointingService.checkAndSaveSnapshot(1L, List.of());

        assertFalse(checkpointingService.getRecentEvents().isEmpty());
        assertFalse(checkpointingService.getRecentSnapshots().isEmpty());

        checkpointingService.clearAll();

        assertTrue(checkpointingService.getRecentEvents().isEmpty());
        assertTrue(checkpointingService.getRecentSnapshots().isEmpty());
    }
}
