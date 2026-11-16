package com.nikitarybalko.wargamebroker.domain.service.impl;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemorySimulatorRegistry implements SimulatorRegistry {

    private final Set<Side> registeredSides = ConcurrentHashMap.newKeySet();

    @Override
    public boolean register(Side side) {
        return registeredSides.add(side);
    }

    @Override
    public boolean isRegistered(Side side) {
        return registeredSides.contains(side);
    }

    @Override
    public Set<Side> getRegisteredSides() {
        return registeredSides.isEmpty() ? EnumSet.noneOf(Side.class) : EnumSet.copyOf(registeredSides);
    }
}
