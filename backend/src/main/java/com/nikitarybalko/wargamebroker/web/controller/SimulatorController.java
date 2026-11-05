package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.web.dto.SimulationStatusResponse;
import com.nikitarybalko.wargamebroker.web.mapper.SimulationStatusMapper;
import com.nikitarybalko.wargamebroker.web.security.ClientRole;
import com.nikitarybalko.wargamebroker.web.security.RequiresRole;
import com.nikitarybalko.wargamebroker.web.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulators")
@RequiredArgsConstructor
@Tag(name = "Simulator Registration", description = "Registration of Blue Force and Red Force simulators")
@SecurityRequirement(name = "ApiKeyAuth")
public class SimulatorController {

    private final SimulationSessionService sessionService;
    private final SimulationStatusMapper statusMapper;

    @Operation(summary = "Register simulator of the authenticated side",
            description = "Registers the simulator identified by its X-API-KEY. Unit submissions are accepted only from registered sides. Idempotent.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulator registered"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key"),
            @ApiResponse(responseCode = "403", description = "Only Blue or Red force keys can register")
    })
    @PostMapping("/register")
    @RequiresRole({ClientRole.ROLE_BLUE, ClientRole.ROLE_RED})
    public ResponseEntity<SimulationStatusResponse> register(HttpServletRequest request) {
        sessionService.registerSimulator(SecurityUtils.getAuthenticatedSide(request));
        return ResponseEntity.ok(statusMapper.currentStatus());
    }
}
