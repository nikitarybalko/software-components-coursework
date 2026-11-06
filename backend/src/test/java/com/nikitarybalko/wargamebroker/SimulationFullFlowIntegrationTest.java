package com.nikitarybalko.wargamebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.support.AbstractPostgresIntegrationTest;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integration tests: End-to-end simulation flow on PostgreSQL")
class SimulationFullFlowIntegrationTest extends AbstractPostgresIntegrationTest {

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

    @Test
    @DisplayName("Snapshot persisted in PostgreSQL restores battlefield and tick counter")
    void testRestoreFromPersistedSnapshot() throws Exception {
        long epochBeforeRestore = currentEpoch();
        for (long tick = 1; tick <= 4; tick++) {
            submitUnit(BLUE_KEY, "blue-1", Side.BLUE, tick, tick * 10.0);
            submitUnit(RED_KEY, "red-1", Side.RED, tick, 300.0 - tick * 10.0);
        }
        submitUnit(BLUE_KEY, "blue-1", Side.BLUE, 5, 999.0);

        String snapshotsJson = mockMvc.perform(get("/api/v1/simulation/state/snapshots")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tickNumber").value(4))
                .andExpect(jsonPath("$[1].tickNumber").value(2))
                .andReturn().getResponse().getContentAsString();
        UUID latestSnapshotId = UUID.fromString(objectMapper.readTree(snapshotsJson).get(0).get("id").asText());

        mockMvc.perform(post("/api/v1/simulation/restore/" + latestSnapshotId)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.currentTick").value(5))
                .andExpect(jsonPath("$.submittedSides.length()").value(0));

        String fullStateJson = mockMvc.perform(get("/api/v1/simulation/state/full")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(40.0, objectMapper.readTree(fullStateJson).get("blueUnits").get(0).get("x").asDouble());
        assertEquals(260.0, objectMapper.readTree(fullStateJson).get("redUnits").get(0).get("x").asDouble());

        mockMvc.perform(get("/api/v1/simulation/ticks/current/units")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epoch").value(epochBeforeRestore + 1))
                .andExpect(jsonPath("$.units.length()").value(1))
                .andExpect(jsonPath("$.units[0].x").value(40.0));

        mockMvc.perform(post("/api/v1/simulation/resume")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        submitUnit(BLUE_KEY, "blue-1", Side.BLUE, 5, 50.0);
        submitUnit(RED_KEY, "red-1", Side.RED, 5, 250.0);

        mockMvc.perform(get("/api/v1/simulation/status")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(jsonPath("$.currentTick").value(6));
    }

    private long currentEpoch() throws Exception {
        String statusJson = mockMvc.perform(get("/api/v1/simulation/status")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(statusJson).get("epoch").asLong();
    }

    private void submitUnit(String apiKey, String unitId, Side side, long tick, double x) throws Exception {
        UnitDto unit = UnitDto.builder()
                .id(unitId)
                .side(side)
                .x(x)
                .y(100.0)
                .viewRadius(30.0)
                .status(UnitStatus.HEALTHY)
                .build();
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(tick)
                .units(List.of(unit))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
