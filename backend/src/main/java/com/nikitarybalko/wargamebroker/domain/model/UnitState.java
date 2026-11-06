package com.nikitarybalko.wargamebroker.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public class UnitState {

    public static final double DAMAGED_VIEW_RADIUS_FACTOR = 0.5;
    public static final double NO_VISION_RADIUS = 0.0;

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
        this.viewRadius = Math.max(NO_VISION_RADIUS, viewRadius);
        this.status = Objects.isNull(status) ? UnitStatus.HEALTHY : status;
    }

    public double getEffectiveViewRadius() {
        return switch (status) {
            case HEALTHY -> viewRadius;
            case DAMAGED -> viewRadius * DAMAGED_VIEW_RADIUS_FACTOR;
            case DESTROYED -> NO_VISION_RADIUS;
        };
    }

    public double distanceTo(UnitState other) {
        Objects.requireNonNull(other, "Target unit must not be null");
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    public boolean isOperational() {
        return status != UnitStatus.DESTROYED;
    }

    public boolean canObserve() {
        return isOperational() && getEffectiveViewRadius() > NO_VISION_RADIUS;
    }
}
