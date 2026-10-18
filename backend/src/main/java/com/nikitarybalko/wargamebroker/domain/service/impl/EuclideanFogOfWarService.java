package com.nikitarybalko.wargamebroker.domain.service.impl;

import org.springframework.stereotype.Service;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.service.FogOfWarService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
public class EuclideanFogOfWarService implements FogOfWarService {

    private static final double EPSILON = 1e-9;

    @Override
    public List<UnitState> calculateVisibleEnemies(List<UnitState> allies, List<UnitState> enemies) {
        if (isEmpty(allies) || isEmpty(enemies) ) {
            return Collections.emptyList();
        }

        List<UnitState> activeObservers = extractActiveObservers(allies);
        if (isEmpty(activeObservers)) {
            return Collections.emptyList();
        }

        return enemies.stream()
                .filter(Objects::nonNull)
                .filter(enemy -> isEnemyDetected(activeObservers, enemy))
                .toList();
    }

    @Override
    public boolean isVisible(UnitState observer, UnitState target) {
        Objects.requireNonNull(observer, "Observer must not be null");
        Objects.requireNonNull(target, "Target must not be null");

        if (!observer.isOperational()) {
            return false;
        }

        double effectiveRadius = observer.getEffectiveViewRadius();
        if (effectiveRadius <= 0.0) {
            return false;
        }

        return observer.distanceTo(target) <= effectiveRadius + EPSILON;
    }

    private List<UnitState> extractActiveObservers(List<UnitState> allies) {
        return allies.stream()
                .filter(Objects::nonNull)
                .filter(UnitState::isOperational)
                .filter(unit -> unit.getEffectiveViewRadius() > 0.0)
                .toList();
    }

    private boolean isEnemyDetected(List<UnitState> observers, UnitState enemy) {
        return observers.stream()
                .anyMatch(observer -> isVisible(observer, enemy));
    }
}
