package com.nikitarybalko.wargamebroker.scheduler;

import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wargame.simulation.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SimulationTimeoutScheduler {

    private final TickManager tickManager;

    @Scheduled(fixedRateString = "${wargame.simulation.timeout-check-interval-ms:1000}")
    public void monitorTickTimeouts() {
        tickManager.checkTimeouts();
    }
}
