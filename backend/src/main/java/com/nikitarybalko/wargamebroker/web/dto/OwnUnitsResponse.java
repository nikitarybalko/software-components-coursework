package com.nikitarybalko.wargamebroker.web.dto;

import com.nikitarybalko.wargamebroker.domain.model.Side;
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
@Schema(description = "Latest unit states of the requesting force as stored by the broker")
public class OwnUnitsResponse {

    @Schema(description = "Current simulation tick number", example = "81")
    private long tickNumber;

    @Schema(description = "State epoch the units belong to", example = "3")
    private long epoch;

    @Schema(description = "Force the units belong to", example = "BLUE")
    private Side side;

    @Schema(description = "Latest known units of the force")
    private List<UnitDto> units;
}
