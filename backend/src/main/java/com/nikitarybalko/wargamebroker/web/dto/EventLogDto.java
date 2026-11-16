package com.nikitarybalko.wargamebroker.web.dto;

import com.nikitarybalko.wargamebroker.domain.model.EventLog;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Simulation event log record")
public class EventLogDto {

    @Schema(description = "Event ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Event timestamp")
    private Instant timestamp;

    @Schema(description = "Type of the event", example = "UNIT_DESTROYED")
    private EventType type;

    @Schema(description = "Faction associated with event (optional)", example = "RED")
    private Side side;

    @Schema(description = "Human-readable description of event", example = "Red unit r-1 was destroyed")
    private String message;

    public static EventLogDto fromDomain(EventLog domain) {
        if (Objects.isNull(domain)) {
            return null;
        }
        return EventLogDto.builder()
                .id(domain.getId())
                .timestamp(domain.getTimestamp())
                .type(domain.getType())
                .side(domain.getSide())
                .message(domain.getMessage())
                .build();
    }
}
