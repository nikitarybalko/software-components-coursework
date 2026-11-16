package com.nikitarybalko.wargamebroker.web.dto;

import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full tactical battlefield overview for commander dashboard and arbitration")
public class FullBattlefieldResponse {

    @Schema(description = "Current simulation tick", example = "3")
    private long currentTick;

    @Schema(description = "Simulation lifecycle state", example = "ACTIVE")
    private SimulationStatus status;

    @Schema(description = "All known units on the Blue force side")
    private List<UnitDto> blueUnits;

    @Schema(description = "All known units on the Red force side")
    private List<UnitDto> redUnits;

    @Schema(description = "Red units currently visible to Blue force")
    private List<UnitDto> blueVisibleRed;

    @Schema(description = "Blue units currently visible to Red force")
    private List<UnitDto> redVisibleBlue;
}
