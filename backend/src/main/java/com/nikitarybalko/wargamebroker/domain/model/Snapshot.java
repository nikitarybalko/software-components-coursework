package com.nikitarybalko.wargamebroker.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
public class Snapshot {

    private final UUID id;
    private final long tickNumber;
    private final Instant createdAt;
    private final List<UnitState> units;

    public Snapshot(UUID id, long tickNumber, Instant createdAt, List<UnitState> units) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tickNumber = tickNumber;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.units = units != null ? List.copyOf(units) : Collections.emptyList();
    }
}
