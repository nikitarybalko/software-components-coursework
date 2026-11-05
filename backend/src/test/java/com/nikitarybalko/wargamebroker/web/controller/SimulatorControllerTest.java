package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.service.SimulationSessionService;
import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.web.mapper.SimulationStatusMapper;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulatorController.class)
@Import(SimulationStatusMapper.class)
@ActiveProfiles("test")
@DisplayName("Web tests: SimulatorController")
class SimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimulationSessionService sessionService;

    @MockitoBean
    private TickManager tickManager;

    @MockitoBean
    private SimulatorRegistry simulatorRegistry;

    @MockitoBean
    private StateCheckpointingService checkpointingService;

    @Test
    @DisplayName("Blue force key registers the Blue simulator")
    void registerBlueSimulator() throws Exception {
        mockMvc.perform(post("/api/v1/simulators/register")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, "blue-secret-token"))
                .andExpect(status().isOk());

        verify(sessionService).registerSimulator(Side.BLUE);
    }

    @Test
    @DisplayName("Red force key registers the Red simulator")
    void registerRedSimulator() throws Exception {
        mockMvc.perform(post("/api/v1/simulators/register")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, "red-secret-token"))
                .andExpect(status().isOk());

        verify(sessionService).registerSimulator(Side.RED);
    }

    @Test
    @DisplayName("Admin key cannot register a simulator")
    void registerWithAdminKeyIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/simulators/register")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, "admin-secret-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sessionService);
    }
}
