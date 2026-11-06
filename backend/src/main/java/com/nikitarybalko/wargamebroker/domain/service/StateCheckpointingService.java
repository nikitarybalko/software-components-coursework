package com.nikitarybalko.wargamebroker.domain.service;

import com.nikitarybalko.wargamebroker.domain.model.EventLog;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StateCheckpointingService {

    int RECENT_SNAPSHOTS_LIMIT = 50;

    int RECENT_EVENTS_LIMIT = 200;

    void checkAndSaveSnapshot(long tickNumber, List<UnitState> units);

    void recordEvent(EventType type, Side side, String message);

    Optional<Snapshot> loadSnapshot(UUID snapshotId);

    List<Snapshot> getRecentSnapshots();

    List<EventLog> getRecentEvents();

    void clearAll();
}
