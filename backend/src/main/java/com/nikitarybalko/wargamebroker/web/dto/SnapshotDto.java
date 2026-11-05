package com.nikitarybalko.wargamebroker.web.dto;

import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Battlefield snapshot record at a specific tick")
public class SnapshotDto {

    @Schema(description = "Snapshot unique ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Simulation tick number", example = "5")
    private long tickNumber;

    @Schema(description = "Timestamp when snapshot was recorded")
    private Instant createdAt;

    @Schema(description = "List of all units present at this tick")
    private List<UnitDto> units;

    public static SnapshotDto fromDomain(Snapshot domain) {
        if (Objects.isNull(domain)) {
            return null;
        }
        List<UnitDto> unitDtos = CollectionUtils.isEmpty(domain.getUnits())
                ? Collections.emptyList()
                : domain.getUnits().stream().map(UnitDto::fromDomain).toList();

        return SnapshotDto.builder()
                .id(domain.getId())
                .tickNumber(domain.getTickNumber())
                .createdAt(domain.getCreatedAt())
                .units(unitDtos)
                .build();
    }
}
