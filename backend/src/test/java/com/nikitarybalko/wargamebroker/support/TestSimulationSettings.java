package com.nikitarybalko.wargamebroker.support;

import com.nikitarybalko.wargamebroker.config.SimulationProperties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Simulation settings matching application-test.yml.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestSimulationSettings {

    public static final long TICK_TIMEOUT_MS = 1000;
    public static final int SNAPSHOT_INTERVAL = 2;
    public static final long TIMEOUT_CHECK_INTERVAL_MS = 1000;
    public static final long FIRST_TICK = 1;
    public static final long SECOND_TICK = FIRST_TICK + 1;
    public static final long EXPIRED_SUBMISSION_AGE_MS = 2 * TICK_TIMEOUT_MS;

    public static SimulationProperties properties() {
        return new SimulationProperties(TICK_TIMEOUT_MS, SNAPSHOT_INTERVAL, TIMEOUT_CHECK_INTERVAL_MS);
    }
}
