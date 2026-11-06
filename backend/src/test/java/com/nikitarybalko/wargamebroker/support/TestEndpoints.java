package com.nikitarybalko.wargamebroker.support;

import com.nikitarybalko.wargamebroker.domain.model.Side;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Endpoint URLs written out literally on purpose: tests guard the public API contract,
 * so they must not reuse the production path constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestEndpoints {

    private static final String SIMULATION = "/api/v1/simulation";
    private static final String TICKS = SIMULATION + "/ticks";
    private static final String STATE = SIMULATION + "/state";
    private static final String SIDE_QUERY = "?side=";

    public static final String STATUS = SIMULATION + "/status";
    public static final String INIT = SIMULATION + "/init";
    public static final String RESET = SIMULATION + "/reset";
    public static final String PAUSE = SIMULATION + "/pause";
    public static final String RESUME = SIMULATION + "/resume";

    public static final String SUBMIT = TICKS + "/submit";
    public static final String CURRENT_TICK = TICKS + "/current";
    public static final String OWN_UNITS = TICKS + "/current/units";

    public static final String STATE_FULL = STATE + "/full";
    public static final String STATE_SNAPSHOTS = STATE + "/snapshots";
    public static final String STATE_EVENTS = STATE + "/events";

    public static final String REGISTER = "/api/v1/simulators/register";

    public static String restore(UUID snapshotId) {
        return SIMULATION + "/restore/" + snapshotId;
    }

    public static String fogOfWar(long tick) {
        return TICKS + "/" + tick + "/fog-of-war";
    }

    public static String fogOfWar(long tick, Side side) {
        return fogOfWar(tick) + SIDE_QUERY + side.name();
    }

    public static String ownUnits(Side side) {
        return OWN_UNITS + SIDE_QUERY + side.name();
    }
}
