package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.config.OpenApiConfig;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.web.ApiPaths;
import com.nikitarybalko.wargamebroker.web.dto.FogOfWarResponse;
import com.nikitarybalko.wargamebroker.web.dto.OwnUnitsResponse;
import com.nikitarybalko.wargamebroker.web.dto.SimulationStatusResponse;
import com.nikitarybalko.wargamebroker.web.dto.SubmitUnitsRequest;
import com.nikitarybalko.wargamebroker.web.dto.SubmitUnitsResponse;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import com.nikitarybalko.wargamebroker.web.mapper.SimulationStatusMapper;
import com.nikitarybalko.wargamebroker.web.security.AnyAuthenticatedRole;
import com.nikitarybalko.wargamebroker.web.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(ApiPaths.SIMULATION_TICKS)
@RequiredArgsConstructor
@Tag(name = "Tick Management", description = "Endpoints for synchronizing simulation steps and retrieving Fog of War intelligence")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AnyAuthenticatedRole
public class TickController {

    private final TickManager tickManager;
    private final SimulationStatusMapper statusMapper;

    @Operation(summary = "Submit unit states for the current tick",
            description = "Submits unit coordinates and status for the given tick. Requires X-API-KEY corresponding to the force.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = ApiResponseDocs.OK, description = "Units successfully accepted"),
            @ApiResponse(responseCode = ApiResponseDocs.BAD_REQUEST, description = "Invalid payload or wrong tick number"),
            @ApiResponse(responseCode = ApiResponseDocs.UNAUTHORIZED, description = ApiResponseDocs.INVALID_API_KEY),
            @ApiResponse(responseCode = ApiResponseDocs.FORBIDDEN, description = "Submitting side does not match API key"),
            @ApiResponse(responseCode = ApiResponseDocs.CONFLICT, description = "Simulation is not active or the side is not registered")
    })
    @PostMapping("/submit")
    public ResponseEntity<SubmitUnitsResponse> submitUnits(
            HttpServletRequest request,
            @Valid @RequestBody SubmitUnitsRequest payload) {

        Side submittingSide = determineAndValidateSubmittingSide(request, payload);
        List<UnitState> domainUnits = payload.getUnits().stream()
                .map(UnitDto::toDomain)
                .toList();

        tickManager.submitUnits(submittingSide, payload.getTickNumber(), domainUnits);

        boolean complete = tickManager.isTickComplete(payload.getTickNumber());
        String msg = complete
                ? "Tick " + payload.getTickNumber() + " complete. Both sides submitted."
                : "Units accepted. Waiting for opponent to submit tick " + payload.getTickNumber() + ".";

        SubmitUnitsResponse response = SubmitUnitsResponse.builder()
                .tickNumber(payload.getTickNumber())
                .simulationStatus(tickManager.getStatus())
                .tickComplete(complete)
                .message(msg)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Retrieve Fog of War intelligence for a specific tick",
            description = "Returns list of visible enemy units for the requesting force within observation radius. Responds 409 until both sides submitted the tick.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = ApiResponseDocs.OK, description = "Intelligence retrieved successfully"),
            @ApiResponse(responseCode = ApiResponseDocs.UNAUTHORIZED, description = ApiResponseDocs.INVALID_API_KEY),
            @ApiResponse(responseCode = ApiResponseDocs.BAD_REQUEST, description = ApiResponseDocs.ADMIN_SIDE_REQUIRED),
            @ApiResponse(responseCode = ApiResponseDocs.FORBIDDEN, description = "Forbidden: Cannot access opponent intelligence"),
            @ApiResponse(responseCode = ApiResponseDocs.NOT_FOUND, description = "Tick data was pruned or never existed"),
            @ApiResponse(responseCode = ApiResponseDocs.CONFLICT, description = "Tick is not complete yet, retry later")
    })
    @GetMapping("/{tickNumber}/fog-of-war")
    public ResponseEntity<FogOfWarResponse> getFogOfWar(
            HttpServletRequest request,
            @PathVariable long tickNumber,
            @RequestParam(required = false) Side side) {

        Side targetSide = resolveRequestedSide(request, side);
        SecurityUtils.validateSideAccess(request, targetSide);

        List<UnitState> visibleEnemies = tickManager.getVisibleEnemies(targetSide, tickNumber);
        List<UnitDto> dtos = visibleEnemies.stream()
                .map(UnitDto::fromDomain)
                .toList();

        FogOfWarResponse response = FogOfWarResponse.builder()
                .tickNumber(tickNumber)
                .side(targetSide)
                .visibleEnemies(dtos)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get current tick status", description = "Returns current simulation tick and status")
    @GetMapping("/current")
    public ResponseEntity<SimulationStatusResponse> getCurrentTickStatus() {
        return ResponseEntity.ok(statusMapper.currentStatus());
    }

    @Operation(summary = "Get own units as known to the broker",
            description = "Returns the latest unit states of the requesting force. After a snapshot restore these are the "
                    + "restored states, so simulators call this endpoint whenever the state epoch changes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = ApiResponseDocs.OK, description = "Own units retrieved successfully"),
            @ApiResponse(responseCode = ApiResponseDocs.BAD_REQUEST, description = ApiResponseDocs.ADMIN_SIDE_REQUIRED),
            @ApiResponse(responseCode = ApiResponseDocs.FORBIDDEN, description = "Forbidden: Cannot access opponent units")
    })
    @GetMapping("/current/units")
    public ResponseEntity<OwnUnitsResponse> getOwnUnits(
            HttpServletRequest request,
            @RequestParam(required = false) Side side) {

        Side targetSide = resolveRequestedSide(request, side);
        SecurityUtils.validateSideAccess(request, targetSide);

        OwnUnitsResponse response = OwnUnitsResponse.builder()
                .tickNumber(tickManager.getCurrentTick())
                .epoch(tickManager.getEpoch())
                .side(targetSide)
                .units(tickManager.getLatestUnits(targetSide).stream().map(UnitDto::fromDomain).toList())
                .build();

        return ResponseEntity.ok(response);
    }

    private Side determineAndValidateSubmittingSide(HttpServletRequest request, SubmitUnitsRequest payload) {
        Side firstUnitSide = payload.getUnits().getFirst().getSide();
        boolean uniformSide = payload.getUnits().stream().allMatch(u -> u.getSide() == firstUnitSide);
        if (!uniformSide) {
            throw new IllegalArgumentException("All submitted units in single request must belong to the same side");
        }

        SecurityUtils.validateSideAccess(request, firstUnitSide);
        return firstUnitSide;
    }

    private Side resolveRequestedSide(HttpServletRequest request, Side requestedSide) {
        if (Objects.nonNull(requestedSide)) {
            return requestedSide;
        }
        Side authenticatedSide = SecurityUtils.getAuthenticatedSide(request);
        if (Objects.isNull(authenticatedSide)) {
            throw new IllegalArgumentException("Query parameter 'side' is required for admin requests");
        }
        return authenticatedSide;
    }
}
