package com.nikitarybalko.wargamebroker.web.dto;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current operational status of the simulation")
public class SimulationStatusResponse {

    @Schema(description = "Current simulation tick number", example = "5")
    private long currentTick;

    @Schema(description = "Lifecycle status of the simulation", example = "ACTIVE")
    private SimulationStatus status;

    @Schema(description = "Whether the current tick has concluded", example = "false")
    private boolean tickComplete;

    @Schema(description = "Sides whose simulators are registered with the broker", example = "[\"BLUE\", \"RED\"]")
    private Set<Side> registeredSides;

    @Schema(description = "Sides that already submitted units for the current tick", example = "[\"BLUE\"]")
    private Set<Side> submittedSides;
}
