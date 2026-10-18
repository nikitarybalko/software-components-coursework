package com.nikitarybalko.wargamebroker.domain.service;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;

import java.util.List;

public interface TickManager {

    long getCurrentTick();

    SimulationStatus getStatus();

    void setStatus(SimulationStatus status);

    void submitUnits(Side side, long tickNumber, List<UnitState> units);

    boolean isTickComplete(long tickNumber);

    List<UnitState> getVisibleEnemies(Side side, long tickNumber);

    List<UnitState> getLatestUnits(Side side);

    void checkTimeouts();

    void reset();

    void restoreState(long tickNumber, List<UnitState> units);
}
