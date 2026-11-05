package com.nikitarybalko.wargamebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.web.dto.SubmitUnitsRequest;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integration tests: End-to-end simulation flow")
class SimulationFullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StateCheckpointingService checkpointingService;

    private static final String BLUE_KEY = "blue-secret-token";
    private static final String RED_KEY = "red-secret-token";
    private static final String ADMIN_KEY = "admin-secret-token";

    @BeforeEach
    void setUp() throws Exception {
        checkpointingService.clearAll();
        mockMvc.perform(post("/api/v1/simulation/init")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk());
        for (String key : List.of(BLUE_KEY, RED_KEY)) {
            mockMvc.perform(post("/api/v1/simulators/register")
                            .header(ApiKeyAuthFilter.API_KEY_HEADER, key))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Complete simulation step with barrier synchronization and Fog of War visibility")
    void testFullSimulationStep() throws Exception {
        // Step 1: Status is ACTIVE at tick 1
        mockMvc.perform(get("/api/v1/simulation/status")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentTick").value(1));

        // Step 2: Blue submits units at tick 1
        UnitDto blueTank = UnitDto.builder()
                .id("blue-1")
                .side(Side.BLUE)
                .x(10.0)
                .y(10.0)
                .viewRadius(50.0)
                .status(UnitStatus.HEALTHY)
                .build();

        SubmitUnitsRequest blueRequest = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(blueTank))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickComplete").value(false));

        // Step 2b: Fog of War is not available until the opponent submits
        mockMvc.perform(get("/api/v1/simulation/ticks/1/fog-of-war")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/simulation/status")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(jsonPath("$.submittedSides[0]").value("BLUE"))
                .andExpect(jsonPath("$.registeredSides.length()").value(2));

        // Step 3: Red submits units at tick 1 (within Blue's view radius: distance ~28.28 <= 50.0)
        UnitDto redTank = UnitDto.builder()
                .id("red-1")
                .side(Side.RED)
                .x(30.0)
                .y(30.0)
                .viewRadius(40.0)
                .status(UnitStatus.HEALTHY)
                .build();

        SubmitUnitsRequest redRequest = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(redTank))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, RED_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(redRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickComplete").value(true));

        // Step 4: Advance to tick 2 after both sides submitted
        mockMvc.perform(get("/api/v1/simulation/ticks/current")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTick").value(2));

        // Step 5: Blue retrieves Fog of War intelligence for tick 1
        mockMvc.perform(get("/api/v1/simulation/ticks/1/fog-of-war")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleEnemies.length()").value(1))
                .andExpect(jsonPath("$.visibleEnemies[0].id").value("red-1"));

        // Step 6: Full battlefield state contains both armies
        mockMvc.perform(get("/api/v1/simulation/state/full")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blueUnits.length()").value(1))
                .andExpect(jsonPath("$.redUnits.length()").value(1))
                .andExpect(jsonPath("$.blueVisibleRed.length()").value(1))
                .andExpect(jsonPath("$.redVisibleBlue.length()").value(1));

        // Step 7: Event log recorded initialization event
        mockMvc.perform(get("/api/v1/simulation/state/events")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Reset clears persisted snapshots and event history")
    void testResetClearsPersistedHistory() throws Exception {
        checkpointingService.checkAndSaveSnapshot(5L, List.of());

        mockMvc.perform(post("/api/v1/simulation/reset")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIALIZING"))
                .andExpect(jsonPath("$.currentTick").value(1));

        mockMvc.perform(get("/api/v1/simulation/state/snapshots")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/simulation/state/events")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("SIMULATION_RESET"));
    }
}
