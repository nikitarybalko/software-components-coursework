package com.nikitarybalko.wargamebroker.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public class UnitState {

    private final String id;
    private final Side side;
    private final double x;
    private final double y;
    private final double viewRadius;
    private final UnitStatus status;

    public UnitState(String id, Side side, double x, double y, double viewRadius, UnitStatus status) {
        this.id = Objects.requireNonNull(id, "Unit ID must not be null");
        this.side = Objects.requireNonNull(side, "Side must not be null");
        this.x = x;
        this.y = y;
        this.viewRadius = Math.max(0.0, viewRadius);
        this.status = status != null ? status : UnitStatus.HEALTHY;
    }

    public double getEffectiveViewRadius() {
        return switch (status) {
            case HEALTHY -> viewRadius;
            case DAMAGED -> viewRadius * 0.5;
            case DESTROYED -> 0.0;
        };
    }

    public double distanceTo(UnitState other) {
        Objects.requireNonNull(other, "Target unit must not be null");
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isOperational() {
        return status != UnitStatus.DESTROYED;
    }
}
