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
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Web tests: StateController")
class StateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TickManager tickManager;

    @MockBean
    private StateCheckpointingService checkpointingService;

    private static final String ADMIN_KEY = "admin-secret-token";

    @Test
    @DisplayName("Get full battlefield state returns units for both factions")
    void testGetFullState() throws Exception {
        UnitState blue = new UnitState("b1", Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.HEALTHY);
        UnitState red = new UnitState("r1", Side.RED, 15.0, 25.0, 40.0, UnitStatus.HEALTHY);

        when(tickManager.getCurrentTick()).thenReturn(2L);
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.getLatestUnits(Side.BLUE)).thenReturn(List.of(blue));
        when(tickManager.getLatestUnits(Side.RED)).thenReturn(List.of(red));
        when(tickManager.getLatestVisibleEnemies(Side.BLUE)).thenReturn(List.of(red));
        when(tickManager.getLatestVisibleEnemies(Side.RED)).thenReturn(List.of(blue));

        mockMvc.perform(get("/api/v1/simulation/state/full")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTick").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.blueUnits.length()").value(1))
                .andExpect(jsonPath("$.redUnits.length()").value(1))
                .andExpect(jsonPath("$.blueVisibleRed.length()").value(1))
                .andExpect(jsonPath("$.redVisibleBlue.length()").value(1));
    }

    @Test
    @DisplayName("Get snapshots returns list of persisted snapshots")
    void testGetSnapshots() throws Exception {
        Snapshot snapshot = new Snapshot(UUID.randomUUID(), 5L, Instant.now(), List.of());
        when(checkpointingService.getRecentSnapshots()).thenReturn(List.of(snapshot));

        mockMvc.perform(get("/api/v1/simulation/state/snapshots")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tickNumber").value(5));
    }

    @Test
    @DisplayName("Get events returns list of system event logs")
    void testGetEvents() throws Exception {
        EventLog event = new EventLog(UUID.randomUUID(), Instant.now(), EventType.UNIT_DESTROYED, Side.RED, "Unit destroyed");
        when(checkpointingService.getRecentEvents()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/simulation/state/events")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("UNIT_DESTROYED"))
                .andExpect(jsonPath("$[0].side").value("RED"));
    }

    @Test
    @DisplayName("State endpoints return 403 for force API keys to keep Fog of War intact")
    void testStateEndpointsForbiddenForForceKeys() throws Exception {
        for (String path : List.of("full", "snapshots", "events")) {
            mockMvc.perform(get("/api/v1/simulation/state/" + path)
                            .header(ApiKeyAuthFilter.API_KEY_HEADER, "red-secret-token"))
                    .andExpect(status().isForbidden());
        }
    }
}
