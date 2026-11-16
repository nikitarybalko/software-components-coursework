package com.nikitarybalko.wargamebroker.web.mapper;

import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.web.dto.SimulationStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimulationStatusMapper {

    private final TickManager tickManager;
    private final SimulatorRegistry simulatorRegistry;

    public SimulationStatusResponse currentStatus() {
        long currentTick = tickManager.getCurrentTick();
        return SimulationStatusResponse.builder()
                .currentTick(currentTick)
                .status(tickManager.getStatus())
                .tickComplete(tickManager.isTickComplete(currentTick))
                .epoch(tickManager.getEpoch())
                .registeredSides(simulatorRegistry.getRegisteredSides())
                .submittedSides(tickManager.getSubmittedSides(currentTick))
                .build();
    }
}
