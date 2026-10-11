package com.nikitarybalko.wargamebroker.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class SimulationTick {

    private final long tickNumber;
    private final Instant timestamp;
    private final SimulationStatus status;
    private final List<UnitState> blueUnits;
    private final List<UnitState> redUnits;

    public SimulationTick(long tickNumber, Instant timestamp, SimulationStatus status,
                          List<UnitState> blueUnits, List<UnitState> redUnits) {
        this.tickNumber = tickNumber;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.status = status != null ? status : SimulationStatus.ACTIVE;
        this.blueUnits = blueUnits != null ? List.copyOf(blueUnits) : Collections.emptyList();
        this.redUnits = redUnits != null ? List.copyOf(redUnits) : Collections.emptyList();
    }

    public List<UnitState> getAllUnits() {
        return java.util.stream.Stream.concat(blueUnits.stream(), redUnits.stream()).toList();
    }
}
