package com.nikitarybalko.wargamebroker.domain.model;

/**
 * Типи критичних подій у життєвому циклі симуляції.
 */
public enum EventType {
    UNIT_DESTROYED,
    UNIT_DAMAGED,
    VALIDATION_ERROR,
    TIMEOUT_DISCONNECT,
    SIMULATION_INITIALIZED,
    SIMULATION_PAUSED,
    SIMULATION_RESUMED,
    SIMULATION_RESET,
    SNAPSHOT_SAVED,
    SNAPSHOT_RESTORED
}
