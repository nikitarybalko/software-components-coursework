package com.nikitarybalko.wargamebroker.domain.model;

/**
 * Сторона конфлікту у симуляторі.
 */
public enum Side {
    BLUE,
    RED;

    public Side opponent() {
        return this == BLUE ? RED : BLUE;
    }
}
