package com.nikitarybalko.wargamebroker.domain.model;

/**
 * Combat function of a unit; drives the NATO APP-6 symbol on the dashboard.
 * Characteristics of each type are defined by the simulators, the broker only stores and routes it.
 */
public enum UnitType {
    ARMOR,
    MECHANIZED_INFANTRY,
    INFANTRY,
    RECON,
    UNKNOWN
}
