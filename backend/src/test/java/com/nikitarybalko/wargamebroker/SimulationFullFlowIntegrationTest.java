package com.nikitarybalko.wargamebroker;

import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.support.AbstractPostgresIntegrationTest;
import com.nikitarybalko.wargamebroker.support.TestApiKeys;
import com.nikitarybalko.wargamebroker.support.TestEndpoints;
import com.nikitarybalko.wargamebroker.web.dto.SubmitUnitsRequest;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.ARRAY_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.BLUE_UNITS_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.BLUE_VISIBLE_RED_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.CURRENT_TICK;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.EPOCH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIELD_BLUE_UNITS;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIELD_EPOCH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIELD_ID;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIELD_RED_UNITS;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIELD_X;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_ITEM_TICK_NUMBER;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_ITEM_TYPE;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_SUBMITTED_SIDE;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_UNIT_X;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_VISIBLE_ENEMY_ID;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.RED_UNITS_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.RED_VISIBLE_BLUE_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.REGISTERED_SIDES_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.SECOND_ITEM_TICK_NUMBER;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.STATUS;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.SUBMITTED_SIDES_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.TICK_COMPLETE;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.UNITS_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.VISIBLE_ENEMIES_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.FIRST_TICK;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.SECOND_TICK;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.SNAPSHOT_INTERVAL;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.BLUE_UNIT;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT;
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

    private static final long LAST_TICK_BEFORE_RESTORE = 2L * SNAPSHOT_INTERVAL;
    private static final double MARCH_STEP = 10.0;
    private static final double RED_START_X = 300.0;
    private static final double MARCH_LINE_Y = 100.0;
    private static final double MARCH_VIEW_RADIUS = 30.0;
    private static final double DISCARDED_BLUE_X = 999.0;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StateCheckpointingService checkpointingService;


    @BeforeEach
    void setUp() throws Exception {
        checkpointingService.clearAll();
        mockMvc.perform(post(TestEndpoints.INIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk());
        for (String key : List.of(TestApiKeys.BLUE, TestApiKeys.RED)) {
            mockMvc.perform(post(TestEndpoints.REGISTER)
                            .header(ApiKeyAuthFilter.API_KEY_HEADER, key))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Complete simulation step with barrier synchronization and Fog of War visibility")
    void testFullSimulationStep() throws Exception {
        // Step 1: Status is ACTIVE at tick 1
        mockMvc.perform(get(TestEndpoints.STATUS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()))
                .andExpect(jsonPath(CURRENT_TICK).value(FIRST_TICK));

        // Step 2: Blue submits units at tick 1
        UnitDto blueTank = UnitDto.builder()
                .id(BLUE_UNIT)
                .side(Side.BLUE)
                .x(10.0)
                .y(10.0)
                .viewRadius(50.0)
                .status(UnitStatus.HEALTHY)
                .build();

        SubmitUnitsRequest blueRequest = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(blueTank))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TICK_COMPLETE).value(false));

        // Step 2b: Fog of War is not available until the opponent submits
        mockMvc.perform(get(TestEndpoints.fogOfWar(FIRST_TICK))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isConflict());

        mockMvc.perform(get(TestEndpoints.STATUS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(jsonPath(FIRST_SUBMITTED_SIDE).value(Side.BLUE.name()))
                .andExpect(jsonPath(REGISTERED_SIDES_LENGTH).value(2));

        // Step 3: Red submits units at tick 1 (within Blue's view radius: distance ~28.28 <= 50.0)
        UnitDto redTank = UnitDto.builder()
                .id(RED_UNIT)
                .side(Side.RED)
                .x(30.0)
                .y(30.0)
                .viewRadius(40.0)
                .status(UnitStatus.HEALTHY)
                .build();

        SubmitUnitsRequest redRequest = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(redTank))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.RED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(redRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TICK_COMPLETE).value(true));

        // Step 4: Advance to tick 2 after both sides submitted
        mockMvc.perform(get(TestEndpoints.CURRENT_TICK)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CURRENT_TICK).value(SECOND_TICK));

        // Step 5: Blue retrieves Fog of War intelligence for tick 1
        mockMvc.perform(get(TestEndpoints.fogOfWar(FIRST_TICK))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(VISIBLE_ENEMIES_LENGTH).value(1))
                .andExpect(jsonPath(FIRST_VISIBLE_ENEMY_ID).value(RED_UNIT));

        // Step 6: Full battlefield state contains both armies
        mockMvc.perform(get(TestEndpoints.STATE_FULL)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(BLUE_UNITS_LENGTH).value(1))
                .andExpect(jsonPath(RED_UNITS_LENGTH).value(1))
                .andExpect(jsonPath(BLUE_VISIBLE_RED_LENGTH).value(1))
                .andExpect(jsonPath(RED_VISIBLE_BLUE_LENGTH).value(1));

        // Step 7: Event log recorded initialization event
        mockMvc.perform(get(TestEndpoints.STATE_EVENTS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ARRAY_LENGTH).value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Reset clears persisted snapshots and event history")
    void testResetClearsPersistedHistory() throws Exception {
        checkpointingService.checkAndSaveSnapshot(SNAPSHOT_INTERVAL, List.of());

        mockMvc.perform(post(TestEndpoints.RESET)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.INITIALIZING.name()))
                .andExpect(jsonPath(CURRENT_TICK).value(FIRST_TICK));

        mockMvc.perform(get(TestEndpoints.STATE_SNAPSHOTS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(jsonPath(ARRAY_LENGTH).value(0));

        mockMvc.perform(get(TestEndpoints.STATE_EVENTS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(jsonPath(ARRAY_LENGTH).value(1))
                .andExpect(jsonPath(FIRST_ITEM_TYPE).value(EventType.SIMULATION_RESET.name()));
    }

    @Test
    @DisplayName("Snapshot persisted in PostgreSQL restores battlefield and tick counter")
    void testRestoreFromPersistedSnapshot() throws Exception {
        long epochBeforeRestore = currentEpoch();
        for (long tick = FIRST_TICK; tick <= LAST_TICK_BEFORE_RESTORE; tick++) {
            submitUnit(TestApiKeys.BLUE, BLUE_UNIT, Side.BLUE, tick, blueX(tick));
            submitUnit(TestApiKeys.RED, RED_UNIT, Side.RED, tick, redX(tick));
        }
        long tickAfterSnapshot = LAST_TICK_BEFORE_RESTORE + 1;
        submitUnit(TestApiKeys.BLUE, BLUE_UNIT, Side.BLUE, tickAfterSnapshot, DISCARDED_BLUE_X);

        String snapshotsJson = mockMvc.perform(get(TestEndpoints.STATE_SNAPSHOTS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ARRAY_LENGTH).value(LAST_TICK_BEFORE_RESTORE / SNAPSHOT_INTERVAL))
                .andExpect(jsonPath(FIRST_ITEM_TICK_NUMBER).value(LAST_TICK_BEFORE_RESTORE))
                .andExpect(jsonPath(SECOND_ITEM_TICK_NUMBER).value(LAST_TICK_BEFORE_RESTORE - SNAPSHOT_INTERVAL))
                .andReturn().getResponse().getContentAsString();
        UUID latestSnapshotId = UUID.fromString(objectMapper.readTree(snapshotsJson).get(0).get(FIELD_ID).asText());

        mockMvc.perform(post(TestEndpoints.restore(latestSnapshotId))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.PAUSED.name()))
                .andExpect(jsonPath(CURRENT_TICK).value(tickAfterSnapshot))
                .andExpect(jsonPath(SUBMITTED_SIDES_LENGTH).value(0));

        String fullStateJson = mockMvc.perform(get(TestEndpoints.STATE_FULL)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(blueX(LAST_TICK_BEFORE_RESTORE),
                objectMapper.readTree(fullStateJson).get(FIELD_BLUE_UNITS).get(0).get(FIELD_X).asDouble());
        assertEquals(redX(LAST_TICK_BEFORE_RESTORE),
                objectMapper.readTree(fullStateJson).get(FIELD_RED_UNITS).get(0).get(FIELD_X).asDouble());

        mockMvc.perform(get(TestEndpoints.OWN_UNITS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(EPOCH).value(epochBeforeRestore + 1))
                .andExpect(jsonPath(UNITS_LENGTH).value(1))
                .andExpect(jsonPath(FIRST_UNIT_X).value(blueX(LAST_TICK_BEFORE_RESTORE)));

        mockMvc.perform(post(TestEndpoints.RESUME)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()));

        submitUnit(TestApiKeys.BLUE, BLUE_UNIT, Side.BLUE, tickAfterSnapshot, blueX(tickAfterSnapshot));
        submitUnit(TestApiKeys.RED, RED_UNIT, Side.RED, tickAfterSnapshot, redX(tickAfterSnapshot));

        mockMvc.perform(get(TestEndpoints.STATUS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(jsonPath(CURRENT_TICK).value(tickAfterSnapshot + 1));
    }

    private static double blueX(long tick) {
        return tick * MARCH_STEP;
    }

    private static double redX(long tick) {
        return RED_START_X - tick * MARCH_STEP;
    }

    private long currentEpoch() throws Exception {
        String statusJson = mockMvc.perform(get(TestEndpoints.STATUS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(statusJson).get(FIELD_EPOCH).asLong();
    }

    private void submitUnit(String apiKey, String unitId, Side side, long tick, double x) throws Exception {
        UnitDto unit = UnitDto.builder()
                .id(unitId)
                .side(side)
                .x(x)
                .y(MARCH_LINE_Y)
                .viewRadius(MARCH_VIEW_RADIUS)
                .status(UnitStatus.HEALTHY)
                .build();
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(tick)
                .units(List.of(unit))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
