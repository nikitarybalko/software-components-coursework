package com.nikitarybalko.wargamebroker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Optional payload for pausing the simulation")
public class PauseRequest {

    @Schema(description = "Reason for pausing", example = "Tactical review by Commander")
    private String reason;
}
