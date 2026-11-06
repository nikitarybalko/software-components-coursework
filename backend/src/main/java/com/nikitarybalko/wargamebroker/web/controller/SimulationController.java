package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.config.OpenApiConfig;
import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.web.ApiPaths;
import com.nikitarybalko.wargamebroker.web.dto.PauseRequest;
import com.nikitarybalko.wargamebroker.web.dto.SimulationStatusResponse;
import com.nikitarybalko.wargamebroker.web.mapper.SimulationStatusMapper;
import com.nikitarybalko.wargamebroker.web.security.ClientRole;
import com.nikitarybalko.wargamebroker.web.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.SIMULATION)
@RequiredArgsConstructor
@Tag(name = "Simulation Lifecycle", description = "Endpoints for controlling the simulation lifecycle (init, pause, resume, reset, restore)")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class SimulationController {

    private final SimulationSessionService sessionService;
    private final SimulationStatusMapper statusMapper;

    @Operation(summary = "Initialize and start simulation", description = "Resets in-memory state and activates the simulation session. Admin only.")
    @PostMapping("/init")
    @RequiresRole(ClientRole.ROLE_ADMIN)
    public ResponseEntity<SimulationStatusResponse> initSimulation() {
        sessionService.initSimulation();
        return ResponseEntity.ok(statusMapper.currentStatus());
    }

    @Operation(summary = "Reset simulation to initial state", description = "Clears in-memory buffers, persisted snapshots and event logs, sets status to INITIALIZING. Admin only.")
    @PostMapping("/reset")
    @RequiresRole(ClientRole.ROLE_ADMIN)
    public ResponseEntity<SimulationStatusResponse> resetSimulation() {
        sessionService.resetSimulation();
        return ResponseEntity.ok(statusMapper.currentStatus());
    }

    @Operation(summary = "Pause simulation", description = "Pauses an ACTIVE simulation. Admin only.")
    @PostMapping("/pause")
    @RequiresRole(ClientRole.ROLE_ADMIN)
    public ResponseEntity<SimulationStatusResponse> pauseSimulation(@RequestBody(required = false) PauseRequest request) {
        String reason = Objects.nonNull(request) ? request.getReason() : null;
        sessionService.pauseSimulation(reason);
        return ResponseEntity.ok(statusMapper.currentStatus());
    }

    @Operation(summary = "Resume paused simulation", description = "Resumes a PAUSED simulation. Admin only.")
    @PostMapping("/resume")
    @RequiresRole(ClientRole.ROLE_ADMIN)
    public ResponseEntity<SimulationStatusResponse> resumeSimulation() {
        sessionService.resumeSimulation();
        return ResponseEntity.ok(statusMapper.currentStatus());
    }

    @Operation(summary = "Restore state from snapshot", description = "Restores units and tick counter from a persisted snapshot and leaves the simulation PAUSED. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = ApiResponseDocs.OK, description = "Snapshot restored successfully"),
            @ApiResponse(responseCode = ApiResponseDocs.FORBIDDEN, description = ApiResponseDocs.ADMIN_KEY_REQUIRED),
            @ApiResponse(responseCode = ApiResponseDocs.NOT_FOUND, description = "Snapshot not found")
    })
    @PostMapping("/restore/{snapshotId}")
    @RequiresRole(ClientRole.ROLE_ADMIN)
    public ResponseEntity<SimulationStatusResponse> restoreFromSnapshot(@PathVariable UUID snapshotId) {
        sessionService.restoreFromSnapshot(snapshotId);
        return ResponseEntity.ok(statusMapper.currentStatus());
    }

    @Operation(summary = "Get simulation status", description = "Returns current simulation lifecycle status and tick")
    @GetMapping("/status")
    public ResponseEntity<SimulationStatusResponse> getStatus() {
        return ResponseEntity.ok(statusMapper.currentStatus());
    }

}
