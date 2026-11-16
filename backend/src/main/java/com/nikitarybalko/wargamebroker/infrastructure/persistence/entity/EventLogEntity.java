package com.nikitarybalko.wargamebroker.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLogEntity {

    public static final int MAX_MESSAGE_LENGTH = 1000;

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private Side side;

    @Column(nullable = false, length = MAX_MESSAGE_LENGTH)
    private String message;
}
