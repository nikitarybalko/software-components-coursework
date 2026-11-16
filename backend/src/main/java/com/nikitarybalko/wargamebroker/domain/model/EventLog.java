package com.nikitarybalko.wargamebroker.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
public class EventLog {

    private final UUID id;
    private final Instant timestamp;
    private final EventType type;
    private final Side side;
    private final String message;

    public EventLog(UUID id, Instant timestamp, EventType type, Side side, String message) {
        this.id = id != null ? id : UUID.randomUUID();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.type = Objects.requireNonNull(type, "EventType must not be null");
        this.side = side;
        this.message = Objects.requireNonNull(message, "Message must not be null");
    }
}
