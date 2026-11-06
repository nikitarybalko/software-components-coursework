package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.domain.model.EventLog;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.Snapshot;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.support.TestApiKeys;
import com.nikitarybalko.wargamebroker.support.TestEndpoints;
import com.nikitarybalko.wargamebroker.support.TestProfiles;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.ARRAY_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.BLUE_UNITS_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.BLUE_VISIBLE_RED_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.CURRENT_TICK;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_BLUE_UNIT_EFFECTIVE_VIEW_RADIUS;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_BLUE_UNIT_VIEW_RADIUS;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_ITEM_SIDE;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_ITEM_TICK_NUMBER;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_ITEM_TYPE;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.RED_UNITS_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.RED_VISIBLE_BLUE_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.STATUS;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.SECOND_TICK;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.BLUE_UNIT;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StateController.class)
@ActiveProfiles(TestProfiles.TEST)
@DisplayName("Web tests: StateController")
class StateControllerTest {

    private static final long SNAPSHOT_TICK = 5;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TickManager tickManager;

    @MockitoBean
    private StateCheckpointingService checkpointingService;


    @Test
    @DisplayName("Get full battlefield state returns units for both factions")
    void testGetFullState() throws Exception {
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.DAMAGED);
        UnitState red = new UnitState(RED_UNIT, Side.RED, 15.0, 25.0, 40.0, UnitStatus.HEALTHY);

        when(tickManager.getCurrentTick()).thenReturn(SECOND_TICK);
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.getLatestUnits(Side.BLUE)).thenReturn(List.of(blue));
        when(tickManager.getLatestUnits(Side.RED)).thenReturn(List.of(red));
        when(tickManager.getLatestVisibleEnemies(Side.BLUE)).thenReturn(List.of(red));
        when(tickManager.getLatestVisibleEnemies(Side.RED)).thenReturn(List.of(blue));

        mockMvc.perform(get(TestEndpoints.STATE_FULL)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CURRENT_TICK).value(SECOND_TICK))
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()))
                .andExpect(jsonPath(BLUE_UNITS_LENGTH).value(1))
                .andExpect(jsonPath(FIRST_BLUE_UNIT_VIEW_RADIUS).value(50.0))
                .andExpect(jsonPath(FIRST_BLUE_UNIT_EFFECTIVE_VIEW_RADIUS).value(25.0))
                .andExpect(jsonPath(RED_UNITS_LENGTH).value(1))
                .andExpect(jsonPath(BLUE_VISIBLE_RED_LENGTH).value(1))
                .andExpect(jsonPath(RED_VISIBLE_BLUE_LENGTH).value(1));
    }

    @Test
    @DisplayName("Get snapshots returns list of persisted snapshots")
    void testGetSnapshots() throws Exception {
        Snapshot snapshot = new Snapshot(UUID.randomUUID(), SNAPSHOT_TICK, Instant.now(), List.of());
        when(checkpointingService.getRecentSnapshots()).thenReturn(List.of(snapshot));

        mockMvc.perform(get(TestEndpoints.STATE_SNAPSHOTS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ARRAY_LENGTH).value(1))
                .andExpect(jsonPath(FIRST_ITEM_TICK_NUMBER).value(SNAPSHOT_TICK));
    }

    @Test
    @DisplayName("Get events returns list of system event logs")
    void testGetEvents() throws Exception {
        EventLog event = new EventLog(UUID.randomUUID(), Instant.now(), EventType.UNIT_DESTROYED, Side.RED, "Unit destroyed");
        when(checkpointingService.getRecentEvents()).thenReturn(List.of(event));

        mockMvc.perform(get(TestEndpoints.STATE_EVENTS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ARRAY_LENGTH).value(1))
                .andExpect(jsonPath(FIRST_ITEM_TYPE).value(EventType.UNIT_DESTROYED.name()))
                .andExpect(jsonPath(FIRST_ITEM_SIDE).value(Side.RED.name()));
    }

    @Test
    @DisplayName("State endpoints return 403 for force API keys to keep Fog of War intact")
    void testStateEndpointsForbiddenForForceKeys() throws Exception {
        for (String endpoint : List.of(TestEndpoints.STATE_FULL, TestEndpoints.STATE_SNAPSHOTS, TestEndpoints.STATE_EVENTS)) {
            mockMvc.perform(get(endpoint)
                            .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.RED))
                    .andExpect(status().isForbidden());
        }
    }
}
