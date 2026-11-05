package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.domain.exception.InvalidLifecycleTransitionException;
import com.nikitarybalko.wargamebroker.domain.exception.SnapshotNotFoundException;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
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
@ActiveProfiles("test")
@DisplayName("Web tests: SimulationController")
class SimulationControllerTest {

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

    private static final String ADMIN_KEY = "admin-secret-token";
    private static final String BLUE_KEY = "blue-secret-token";

    @Test
    @DisplayName("Init simulation endpoint initializes session and returns 200")
    void testInitSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.getCurrentTick()).thenReturn(1L);

        mockMvc.perform(post("/api/v1/simulation/init")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentTick").value(1));

        verify(sessionService).initSimulation();
    }

    @Test
    @DisplayName("Reset simulation endpoint resets session and returns 200")
    void testResetSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.INITIALIZING);
        when(tickManager.getCurrentTick()).thenReturn(1L);

        mockMvc.perform(post("/api/v1/simulation/reset")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIALIZING"));

        verify(sessionService).resetSimulation();
    }

    @Test
    @DisplayName("Pause simulation endpoint pauses session with reason")
    void testPauseSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.PAUSED);
        PauseRequest request = PauseRequest.builder().reason("Testing pause").build();

        mockMvc.perform(post("/api/v1/simulation/pause")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(sessionService).pauseSimulation("Testing pause");
    }

    @Test
    @DisplayName("Resume simulation endpoint resumes session")
    void testResumeSimulation() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/simulation/resume")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(sessionService).resumeSimulation();
    }

    @Test
    @DisplayName("Restore simulation from snapshot restores state and returns 200")
    void testRestoreFromSnapshot() throws Exception {
        UUID snapshotId = UUID.randomUUID();
        when(tickManager.getStatus()).thenReturn(SimulationStatus.PAUSED);
        when(tickManager.getCurrentTick()).thenReturn(6L);

        mockMvc.perform(post("/api/v1/simulation/restore/" + snapshotId)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.currentTick").value(6));

        verify(sessionService).restoreFromSnapshot(snapshotId);
    }

    @Test
    @DisplayName("Status endpoint returns current simulation status")
    void testGetStatus() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.getCurrentTick()).thenReturn(4L);

        mockMvc.perform(get("/api/v1/simulation/status")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentTick").value(4));
    }

    @Test
    @DisplayName("Lifecycle endpoints return 403 for force API keys")
    void testLifecycleEndpointsForbiddenForForceKeys() throws Exception {
        for (String action : List.of("init", "reset", "pause", "resume", "restore/" + UUID.randomUUID())) {
            mockMvc.perform(post("/api/v1/simulation/" + action)
                            .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("Status endpoint is available to force API keys")
    void testStatusAvailableForForceKeys() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/simulation/status")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Resume returns 409 when lifecycle transition is invalid")
    void testResumeInvalidTransitionReturnsConflict() throws Exception {
        doThrow(new InvalidLifecycleTransitionException("resume", SimulationStatus.ACTIVE))
                .when(sessionService).resumeSimulation();

        mockMvc.perform(post("/api/v1/simulation/resume")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Restore returns 404 when snapshot does not exist")
    void testRestoreMissingSnapshotReturnsNotFound() throws Exception {
        UUID snapshotId = UUID.randomUUID();
        doThrow(new SnapshotNotFoundException(snapshotId)).when(sessionService).restoreFromSnapshot(snapshotId);

        mockMvc.perform(post("/api/v1/simulation/restore/" + snapshotId)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isNotFound());
    }
}
