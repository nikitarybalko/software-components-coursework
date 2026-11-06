package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.domain.exception.InvalidLifecycleTransitionException;
import com.nikitarybalko.wargamebroker.domain.exception.SnapshotNotFoundException;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.support.TestApiKeys;
import com.nikitarybalko.wargamebroker.support.TestEndpoints;
import com.nikitarybalko.wargamebroker.support.TestProfiles;
import com.nikitarybalko.wargamebroker.web.dto.PauseRequest;
import com.nikitarybalko.wargamebroker.web.mapper.SimulationStatusMapper;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.CURRENT_TICK;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.STATUS;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.FIRST_TICK;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulationController.class)
@Import(SimulationStatusMapper.class)
@ActiveProfiles(TestProfiles.TEST)
@DisplayName("Web tests: SimulationController")
class SimulationControllerTest {

    private static final long ONGOING_TICK = 4;
    private static final long TICK_AFTER_RESTORE = 6;

    private static final String PAUSE_REASON = "Tactical review";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SimulationSessionService sessionService;

    @MockitoBean
    private TickManager tickManager;

    @MockitoBean
    private SimulatorRegistry simulatorRegistry;

    @MockitoBean
    private StateCheckpointingService checkpointingService;


    @Test
    @DisplayName("Init simulation endpoint initializes session and returns 200")
    void testInitSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.getCurrentTick()).thenReturn(FIRST_TICK);

        mockMvc.perform(post(TestEndpoints.INIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()))
                .andExpect(jsonPath(CURRENT_TICK).value(FIRST_TICK));

        verify(sessionService).initSimulation();
    }

    @Test
    @DisplayName("Reset simulation endpoint resets session and returns 200")
    void testResetSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.INITIALIZING);
        when(tickManager.getCurrentTick()).thenReturn(FIRST_TICK);

        mockMvc.perform(post(TestEndpoints.RESET)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.INITIALIZING.name()));

        verify(sessionService).resetSimulation();
    }

    @Test
    @DisplayName("Pause simulation endpoint pauses session with reason")
    void testPauseSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.PAUSED);
        PauseRequest request = PauseRequest.builder().reason(PAUSE_REASON).build();

        mockMvc.perform(post(TestEndpoints.PAUSE)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.PAUSED.name()));

        verify(sessionService).pauseSimulation(PAUSE_REASON);
    }

    @Test
    @DisplayName("Resume simulation endpoint resumes session")
    void testResumeSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);

        mockMvc.perform(post(TestEndpoints.RESUME)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()));

        verify(sessionService).resumeSimulation();
    }

    @Test
    @DisplayName("Restore simulation from snapshot restores state and returns 200")
    void testRestoreFromSnapshot() throws Exception {
        UUID snapshotId = UUID.randomUUID();
        when(tickManager.getStatus()).thenReturn(SimulationStatus.PAUSED);
        when(tickManager.getCurrentTick()).thenReturn(TICK_AFTER_RESTORE);

        mockMvc.perform(post(TestEndpoints.restore(snapshotId))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.PAUSED.name()))
                .andExpect(jsonPath(CURRENT_TICK).value(TICK_AFTER_RESTORE));

        verify(sessionService).restoreFromSnapshot(snapshotId);
    }

    @Test
    @DisplayName("Status endpoint returns current simulation status")
    void testGetStatus() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.getCurrentTick()).thenReturn(ONGOING_TICK);

        mockMvc.perform(get(TestEndpoints.STATUS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()))
                .andExpect(jsonPath(CURRENT_TICK).value(ONGOING_TICK));
    }

    @Test
    @DisplayName("Lifecycle endpoints return 403 for force API keys")
    void testLifecycleEndpointsForbiddenForForceKeys() throws Exception {
        for (String endpoint : List.of(TestEndpoints.INIT, TestEndpoints.RESET, TestEndpoints.PAUSE,
                TestEndpoints.RESUME, TestEndpoints.restore(UUID.randomUUID()))) {
            mockMvc.perform(post(endpoint)
                            .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("Status endpoint is available to force API keys")
    void testStatusAvailableForForceKeys() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);

        mockMvc.perform(get(TestEndpoints.STATUS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("Resume returns 409 when lifecycle transition is invalid")
    void testResumeInvalidTransitionReturnsConflict() throws Exception {
        doThrow(new InvalidLifecycleTransitionException("resume", SimulationStatus.ACTIVE))
                .when(sessionService).resumeSimulation();

        mockMvc.perform(post(TestEndpoints.RESUME)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Restore returns 404 when snapshot does not exist")
    void testRestoreMissingSnapshotReturnsNotFound() throws Exception {
        UUID snapshotId = UUID.randomUUID();
        doThrow(new SnapshotNotFoundException(snapshotId)).when(sessionService).restoreFromSnapshot(snapshotId);

        mockMvc.perform(post(TestEndpoints.restore(snapshotId))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isNotFound());
    }
}
