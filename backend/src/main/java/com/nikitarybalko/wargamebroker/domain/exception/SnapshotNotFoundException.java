package com.nikitarybalko.wargamebroker.domain.exception;

import java.util.UUID;

public class SnapshotNotFoundException extends SimulationException {
    public SnapshotNotFoundException(UUID snapshotId) {
        super("Snapshot not found with ID: " + snapshotId);
    }
}
