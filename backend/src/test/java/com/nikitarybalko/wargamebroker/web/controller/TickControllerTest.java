package com.nikitarybalko.wargamebroker.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikitarybalko.wargamebroker.domain.exception.SimulatorNotRegisteredException;
import com.nikitarybalko.wargamebroker.domain.exception.TickNotCompleteException;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.web.dto.SubmitUnitsRequest;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Web tests: TickController")
class TickControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TickManager tickManager;

    @MockBean
    private StateCheckpointingService checkpointingService;

    private static final String BLUE_KEY = "blue-secret-token";
    private static final String RED_KEY = "red-secret-token";
    private static final String ADMIN_KEY = "admin-secret-token";

    @Test
    @DisplayName("Submit units fails with 401 when X-API-KEY header is missing")
    void submitUnitsWithoutApiKeyReturns401() throws Exception {
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(createUnitDto("b1", Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Submit units fails with 401 when X-API-KEY is invalid")
    void submitUnitsWithInvalidApiKeyReturns401() throws Exception {
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(createUnitDto("b1", Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Submit units succeeds with 200 when Blue submits Blue units")
    void submitUnitsBlueSuccess() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.isTickComplete(1L)).thenReturn(false);

        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(createUnitDto("b1", Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickNumber").value(1))
                .andExpect(jsonPath("$.tickComplete").value(false));

        verify(tickManager).submitUnits(eq(Side.BLUE), eq(1L), any());
    }

    @Test
    @DisplayName("Submit units fails with 403 when Blue attempts to submit Red units (anti-spoofing)")
    void submitUnitsBlueSpoofingRedFails() throws Exception {
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(createUnitDto("r1", Side.RED, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Submit units fails with 400 when payload validation fails (negative view radius)")
    void submitUnitsValidationFailure() throws Exception {
        UnitDto invalidUnit = createUnitDto("b1", Side.BLUE, 10.0, 20.0, -5.0, UnitStatus.HEALTHY);
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(invalidUnit))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Fog of War endpoint returns 200 with visible enemies for Blue")
    void getFogOfWarBlueSuccess() throws Exception {
        UnitState redSpotted = new UnitState("r1", Side.RED, 12.0, 22.0, 40.0, UnitStatus.HEALTHY);
        when(tickManager.getVisibleEnemies(Side.BLUE, 1L)).thenReturn(List.of(redSpotted));

        mockMvc.perform(get("/api/v1/simulation/ticks/1/fog-of-war")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickNumber").value(1))
                .andExpect(jsonPath("$.side").value("BLUE"))
                .andExpect(jsonPath("$.visibleEnemies.length()").value(1))
                .andExpect(jsonPath("$.visibleEnemies[0].id").value("r1"));
    }

    @Test
    @DisplayName("Fog of War endpoint returns 403 when Blue requests Red intelligence")
    void getFogOfWarBlueForbiddenForRed() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/ticks/1/fog-of-war?side=RED")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin API key can request Fog of War for either side")
    void getFogOfWarAdminAccess() throws Exception {
        when(tickManager.getVisibleEnemies(Side.RED, 1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/simulation/ticks/1/fog-of-war?side=RED")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.side").value("RED"));
    }

    @Test
    @DisplayName("Current tick status endpoint returns 200")
    void getCurrentTickStatus() throws Exception {
        when(tickManager.getCurrentTick()).thenReturn(3L);
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.isTickComplete(3L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/simulation/ticks/current")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTick").value(3))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Fog of War endpoint returns 409 while tick is not complete")
    void getFogOfWarForIncompleteTickReturnsConflict() throws Exception {
        when(tickManager.getVisibleEnemies(Side.BLUE, 2L)).thenThrow(new TickNotCompleteException(2L));

        mockMvc.perform(get("/api/v1/simulation/ticks/2/fog-of-war")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, BLUE_KEY))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Admin Fog of War request without side parameter returns 400")
    void getFogOfWarAdminWithoutSideReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/ticks/1/fog-of-war")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Submit units returns 409 when simulator is not registered")
    void submitUnitsFromUnregisteredSimulatorReturnsConflict() throws Exception {
        doThrow(new SimulatorNotRegisteredException(Side.RED))
                .when(tickManager).submitUnits(eq(Side.RED), eq(1L), any());
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(1L)
                .units(List.of(createUnitDto("r1", Side.RED, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post("/api/v1/simulation/ticks/submit")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, RED_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    private UnitDto createUnitDto(String id, Side side, double x, double y, double radius, UnitStatus status) {
        return UnitDto.builder()
                .id(id)
                .side(side)
                .x(x)
                .y(y)
                .viewRadius(radius)
                .status(status)
                .build();
    }
}
