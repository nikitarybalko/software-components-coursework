package com.nikitarybalko.wargamebroker.domain.service;

import com.nikitarybalko.wargamebroker.domain.model.Side;

import java.util.Set;

public interface SimulatorRegistry {

    /**
     * @return true if the side was not registered before this call
     */
    boolean register(Side side);

    boolean isRegistered(Side side);

    Set<Side> getRegisteredSides();
}
