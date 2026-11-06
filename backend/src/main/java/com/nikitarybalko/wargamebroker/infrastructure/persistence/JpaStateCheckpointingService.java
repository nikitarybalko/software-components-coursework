package com.nikitarybalko.wargamebroker.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nikitarybalko.wargamebroker.domain.model.EventLog;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.EventLogEntity;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.SnapshotEntity;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.SnapshotUnitEntity;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.repository.EventLogRepository;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.repository.SnapshotRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class JpaStateCheckpointingService implements StateCheckpointingService {

    private static final String TRUNCATION_MARKER = "...";

    private final SnapshotRepository snapshotRepository;
    private final EventLogRepository eventLogRepository;

    @Override
    @Transactional
    public void checkAndSaveSnapshot(long tickNumber, List<UnitState> units) {
        UUID snapshotId = UUID.randomUUID();
        SnapshotEntity entity = SnapshotEntity.builder()
                .id(snapshotId)
                .tickNumber(tickNumber)
                .createdAt(Instant.now())
                .build();

        List<SnapshotUnitEntity> unitEntities = mapToUnitEntities(units, entity);
        entity.setUnits(unitEntities);

        snapshotRepository.save(entity);
        log.info("Saved JPA snapshot {} for tick {} with {} units", snapshotId, tickNumber, unitEntities.size());

        recordEvent(EventType.SNAPSHOT_SAVED, null,
                "Snapshot saved for tick " + tickNumber + " with " + unitEntities.size() + " units");
    }

    @Override
    @Transactional
    public void recordEvent(EventType type, Side side, String message) {
        EventLogEntity entity = EventLogEntity.builder()
                .id(UUID.randomUUID())
                .timestamp(Instant.now())
                .eventType(type)
                .side(side)
                .message(fitToColumn(message))
                .build();

        eventLogRepository.save(entity);
        log.debug("Saved JPA event log: [{}] side={} message={}", type, side, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Snapshot> loadSnapshot(UUID snapshotId) {
        return snapshotRepository.findById(snapshotId)
                .map(this::toDomainSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Snapshot> getRecentSnapshots() {
        return snapshotRepository.findAllByOrderByTickNumberDesc(Limit.of(RECENT_SNAPSHOTS_LIMIT)).stream()
                .map(this::toDomainSnapshot)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventLog> getRecentEvents() {
        return eventLogRepository.findAllByOrderByTimestampDesc(Limit.of(RECENT_EVENTS_LIMIT)).stream()
                .map(this::toDomainEvent)
                .toList();
    }

    @Override
    @Transactional
    public void clearAll() {
        snapshotRepository.deleteAll();
        eventLogRepository.deleteAll();
        log.info("Cleared all snapshots and event logs from database");
    }

    private String fitToColumn(String message) {
        if (message.length() <= EventLogEntity.MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, EventLogEntity.MAX_MESSAGE_LENGTH - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
    }

    private List<SnapshotUnitEntity> mapToUnitEntities(List<UnitState> units, SnapshotEntity parent) {
        if (units == null) return Collections.emptyList();
        return units.stream()
                .map(unit -> toUnitEntity(unit, parent))
                .toList();
    }

    private SnapshotUnitEntity toUnitEntity(UnitState unit, SnapshotEntity parent) {
        return SnapshotUnitEntity.builder()
                .id(UUID.randomUUID())
                .snapshot(parent)
                .unitId(unit.getId())
                .side(unit.getSide())
                .x(unit.getX())
                .y(unit.getY())
                .viewRadius(unit.getViewRadius())
                .status(unit.getStatus())
                .build();
    }

    private Snapshot toDomainSnapshot(SnapshotEntity entity) {
        List<UnitState> domainUnits = entity.getUnits().stream()
                .map(this::toDomainUnit)
                .toList();

        return new Snapshot(entity.getId(), entity.getTickNumber(), entity.getCreatedAt(), domainUnits);
    }

    private UnitState toDomainUnit(SnapshotUnitEntity entity) {
        return new UnitState(
                entity.getUnitId(),
                entity.getSide(),
                entity.getX(),
                entity.getY(),
                entity.getViewRadius(),
                entity.getStatus()
        );
    }

    private EventLog toDomainEvent(EventLogEntity entity) {
        return new EventLog(
                entity.getId(),
                entity.getTimestamp(),
                entity.getEventType(),
                entity.getSide(),
                entity.getMessage()
        );
    }
}
