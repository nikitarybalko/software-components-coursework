package com.nikitarybalko.wargamebroker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for submitting tick state of a side's units")
public class SubmitUnitsRequest {

    @NotNull(message = "Tick number cannot be null")
    @Positive(message = "Tick number must be greater than zero")
    @Schema(description = "Simulation tick number", example = "1")
    private Long tickNumber;

    @NotEmpty(message = "Units list cannot be empty")
    @Valid
    @Schema(description = "List of tactical units controlled by the submitting side")
    private List<UnitDto> units;
}
