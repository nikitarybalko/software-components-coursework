package com.nikitarybalko.wargamebroker.domain.service;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;

import java.util.List;
import java.util.Set;

public interface TickManager {

    long getCurrentTick();

    SimulationStatus getStatus();

    void setStatus(SimulationStatus status);

    void submitUnits(Side side, long tickNumber, List<UnitState> units);

    boolean isTickComplete(long tickNumber);

    Set<Side> getSubmittedSides(long tickNumber);

    /**
     * @throws com.nikitarybalko.wargamebroker.domain.exception.TickNotCompleteException
     *         if the tick has not been processed yet
     * @throws com.nikitarybalko.wargamebroker.domain.exception.TickDataUnavailableException
     *         if the tick data was pruned or never existed
     */
    List<UnitState> getVisibleEnemies(Side side, long tickNumber);

    List<UnitState> getLatestVisibleEnemies(Side side);

    List<UnitState> getLatestUnits(Side side);

    void checkTimeouts();

    void reset();

    void restoreState(long tickNumber, List<UnitState> units);
}
