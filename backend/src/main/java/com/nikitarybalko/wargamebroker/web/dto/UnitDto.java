package com.nikitarybalko.wargamebroker.web.dto;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tactical battlefield unit state")
public class UnitDto {

    @NotBlank(message = "Unit identifier cannot be blank")
    @Schema(description = "Unique identifier of the unit", example = "blue-tank-1")
    private String id;

    @NotNull(message = "Unit side cannot be null")
    @Schema(description = "Faction side of the unit", example = "BLUE")
    private Side side;

    @NotNull(message = "X coordinate cannot be null")
    @Schema(description = "Continuous 2D X coordinate", example = "105.5")
    private Double x;

    @NotNull(message = "Y coordinate cannot be null")
    @Schema(description = "Continuous 2D Y coordinate", example = "230.0")
    private Double y;

    @NotNull(message = "View radius cannot be null")
    @PositiveOrZero(message = "View radius must be non-negative")
    @Schema(description = "Sensor/vision radius in meters", example = "75.0")
    private Double viewRadius;

    @NotNull(message = "Unit status cannot be null")
    @Schema(description = "Operational combat status of the unit", example = "HEALTHY")
    private UnitStatus status;

    @Schema(description = "View radius after status penalties (DAMAGED halves it, DESTROYED zeroes it). Computed by the broker, ignored on input",
            example = "37.5", accessMode = Schema.AccessMode.READ_ONLY)
    private Double effectiveViewRadius;

    public static UnitDto fromDomain(UnitState domain) {
        if (domain == null) {
            return null;
        }
        return UnitDto.builder()
                .id(domain.getId())
                .side(domain.getSide())
                .x(domain.getX())
                .y(domain.getY())
                .viewRadius(domain.getViewRadius())
                .status(domain.getStatus())
                .effectiveViewRadius(domain.getEffectiveViewRadius())
                .build();
    }

    public UnitState toDomain() {
        return new UnitState(this.id, this.side, this.x, this.y, this.viewRadius, this.status);
    }
}
