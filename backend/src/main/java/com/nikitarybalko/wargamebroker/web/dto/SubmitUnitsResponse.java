package com.nikitarybalko.wargamebroker.web.dto;

import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response confirming unit submission for the tick")
public class SubmitUnitsResponse {

    @Schema(description = "Processed tick number", example = "1")
    private long tickNumber;

    @Schema(description = "Current simulation lifecycle status", example = "ACTIVE")
    private SimulationStatus simulationStatus;

    @Schema(description = "Whether both sides have submitted and tick processing is finalized", example = "true")
    private boolean tickComplete;

    @Schema(description = "Human-readable status message", example = "Units submitted successfully")
    private String message;
}
