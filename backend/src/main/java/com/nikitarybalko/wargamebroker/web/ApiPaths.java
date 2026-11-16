package com.nikitarybalko.wargamebroker.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

    public static final String API_PATTERN = "/api/**";
    public static final String API_V1 = "/api/v1";
    public static final String SIMULATION = API_V1 + "/simulation";
    public static final String SIMULATION_STATE = SIMULATION + "/state";
    public static final String SIMULATION_TICKS = SIMULATION + "/ticks";
    public static final String SIMULATORS = API_V1 + "/simulators";
}
