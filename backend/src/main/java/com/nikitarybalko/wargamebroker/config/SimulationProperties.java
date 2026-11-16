package com.nikitarybalko.wargamebroker.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "wargame.simulation")
public record SimulationProperties(
        @Positive long tickTimeoutMs,
        @Positive int snapshotInterval,
        @Positive long timeoutCheckIntervalMs) {
}
