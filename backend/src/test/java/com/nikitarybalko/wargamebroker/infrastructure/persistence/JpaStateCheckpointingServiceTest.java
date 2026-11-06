package com.nikitarybalko.wargamebroker.infrastructure.persistence;

import com.nikitarybalko.wargamebroker.domain.model.EventLog;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitType;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.EventLogEntity;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.repository.EventLogRepository;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.repository.SnapshotRepository;
import com.nikitarybalko.wargamebroker.support.AbstractPostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.FIRST_TICK;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.BLUE_UNIT;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Integration tests: Persistence & Checkpointing on PostgreSQL (JpaStateCheckpointingService)")
class JpaStateCheckpointingServiceTest extends AbstractPostgresIntegrationTest {

    private static final long SNAPSHOT_TICK = 5;
    private static final String TRUNCATION_MARKER = "...";

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
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 10.5, 20.5, 50.0, UnitStatus.HEALTHY);
        UnitState red = new UnitState(RED_UNIT, Side.RED, 15.0, 25.0, 40.0, UnitStatus.DAMAGED);

        checkpointingService.checkAndSaveSnapshot(SNAPSHOT_TICK, List.of(blue, red));

        List<Snapshot> snapshots = checkpointingService.getRecentSnapshots();
        assertEquals(1, snapshots.size());

        Snapshot savedSnapshot = snapshots.get(0);
        assertEquals(SNAPSHOT_TICK, savedSnapshot.getTickNumber());
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
        checkpointingService.checkAndSaveSnapshot(FIRST_TICK, List.of());

        assertFalse(checkpointingService.getRecentEvents().isEmpty());
        assertFalse(checkpointingService.getRecentSnapshots().isEmpty());

        checkpointingService.clearAll();

        assertTrue(checkpointingService.getRecentEvents().isEmpty());
        assertTrue(checkpointingService.getRecentSnapshots().isEmpty());
    }

    @Test
    @DisplayName("Snapshot keeps unit types after being reloaded from the database")
    void testSnapshotPersistsUnitTypes() {
        UnitState recon = new UnitState(BLUE_UNIT, Side.BLUE, UnitType.RECON, 10.0, 20.0, 85.0, UnitStatus.HEALTHY);
        UnitState untyped = new UnitState(RED_UNIT, Side.RED, 30.0, 40.0, 50.0, UnitStatus.HEALTHY);

        checkpointingService.checkAndSaveSnapshot(FIRST_TICK, List.of(recon, untyped));

        Snapshot reloaded = checkpointingService.loadSnapshot(checkpointingService.getRecentSnapshots().get(0).getId())
                .orElseThrow();
        assertEquals(UnitType.RECON, findUnit(reloaded, BLUE_UNIT).getType());
        assertEquals(UnitType.UNKNOWN, findUnit(reloaded, RED_UNIT).getType());
    }

    @Test
    @DisplayName("recordEvent truncates messages longer than the column limit instead of failing")
    void testRecordEventTruncatesLongMessage() {
        String oversizedMessage = "x".repeat(EventLogEntity.MAX_MESSAGE_LENGTH * 2);

        checkpointingService.recordEvent(EventType.VALIDATION_ERROR, null, oversizedMessage);

        String storedMessage = checkpointingService.getRecentEvents().get(0).getMessage();
        assertEquals(EventLogEntity.MAX_MESSAGE_LENGTH, storedMessage.length());
        assertTrue(storedMessage.endsWith(TRUNCATION_MARKER));
    }

    private UnitState findUnit(Snapshot snapshot, String unitId) {
        return snapshot.getUnits().stream()
                .filter(unit -> unit.getId().equals(unitId))
                .findFirst()
                .orElseThrow();
    }
}
