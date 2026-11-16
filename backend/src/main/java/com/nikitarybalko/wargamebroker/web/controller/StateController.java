package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.config.OpenApiConfig;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.web.ApiPaths;
import com.nikitarybalko.wargamebroker.web.dto.EventLogDto;
import com.nikitarybalko.wargamebroker.web.dto.FullBattlefieldResponse;
import com.nikitarybalko.wargamebroker.web.dto.SnapshotDto;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import com.nikitarybalko.wargamebroker.web.security.ClientRole;
import com.nikitarybalko.wargamebroker.web.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.SIMULATION_STATE)
@RequiredArgsConstructor
@Tag(name = "State & History", description = "Admin-only endpoints for inspecting battlefield overview, snapshots and event logs")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RequiresRole(ClientRole.ROLE_ADMIN)
public class StateController {

    private final TickManager tickManager;
    private final StateCheckpointingService checkpointingService;

    @Operation(summary = "Get full battlefield overview",
            description = "Returns current coordinates and vision for both sides (useful for Commander Dashboard and Arbiter mode)")
    @GetMapping("/full")
    public ResponseEntity<FullBattlefieldResponse> getFullState() {
        long currentTick = tickManager.getCurrentTick();

        List<UnitDto> blueUnits = tickManager.getLatestUnits(Side.BLUE).stream()
                .map(UnitDto::fromDomain)
                .toList();

        List<UnitDto> redUnits = tickManager.getLatestUnits(Side.RED).stream()
                .map(UnitDto::fromDomain)
                .toList();

        List<UnitDto> blueSpotsRed = tickManager.getLatestVisibleEnemies(Side.BLUE).stream()
                .map(UnitDto::fromDomain)
                .toList();

        List<UnitDto> redSpotsBlue = tickManager.getLatestVisibleEnemies(Side.RED).stream()
                .map(UnitDto::fromDomain)
                .toList();

        FullBattlefieldResponse response = FullBattlefieldResponse.builder()
                .currentTick(currentTick)
                .status(tickManager.getStatus())
                .blueUnits(blueUnits)
                .redUnits(redUnits)
                .blueVisibleRed(blueSpotsRed)
                .redVisibleBlue(redSpotsBlue)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get recent snapshots", description = "Returns up to " + StateCheckpointingService.RECENT_SNAPSHOTS_LIMIT
            + " latest persisted snapshots ordered by tick number descending")
    @GetMapping("/snapshots")
    public ResponseEntity<List<SnapshotDto>> getSnapshots() {
        List<SnapshotDto> snapshots = checkpointingService.getRecentSnapshots().stream()
                .map(SnapshotDto::fromDomain)
                .toList();
        return ResponseEntity.ok(snapshots);
    }

    @Operation(summary = "Get recent event logs", description = "Returns up to " + StateCheckpointingService.RECENT_EVENTS_LIMIT
            + " latest system event logs ordered chronologically descending")
    @GetMapping("/events")
    public ResponseEntity<List<EventLogDto>> getEventLogs() {
        List<EventLogDto> events = checkpointingService.getRecentEvents().stream()
                .map(EventLogDto::fromDomain)
                .toList();
        return ResponseEntity.ok(events);
    }
}
