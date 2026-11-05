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
@Schema(description = "Fog of War intelligence data for a requesting side")
public class FogOfWarResponse {

    @Schema(description = "Requested tick number", example = "1")
    private long tickNumber;

    @Schema(description = "Side for which intelligence is generated", example = "BLUE")
    private Side side;

    @Schema(description = "List of enemy units currently detected in observation range")
    private List<UnitDto> visibleEnemies;
}
