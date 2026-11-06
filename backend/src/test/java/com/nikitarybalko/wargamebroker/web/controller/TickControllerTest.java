package com.nikitarybalko.wargamebroker.web.controller;

import com.nikitarybalko.wargamebroker.domain.exception.SimulatorNotRegisteredException;
import com.nikitarybalko.wargamebroker.domain.exception.TickNotCompleteException;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.SimulatorRegistry;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.domain.service.TickManager;
import com.nikitarybalko.wargamebroker.support.TestApiKeys;
import com.nikitarybalko.wargamebroker.support.TestEndpoints;
import com.nikitarybalko.wargamebroker.support.TestProfiles;
import com.nikitarybalko.wargamebroker.web.dto.SubmitUnitsRequest;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import com.nikitarybalko.wargamebroker.web.mapper.SimulationStatusMapper;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.CURRENT_TICK;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.EPOCH;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.ERROR;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_UNIT_ID;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_UNIT_STATUS;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.FIRST_VISIBLE_ENEMY_ID;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.SIDE;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.STATUS;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.TICK_COMPLETE;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.TICK_NUMBER;
import static com.nikitarybalko.wargamebroker.support.TestJsonPaths.VISIBLE_ENEMIES_LENGTH;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.FIRST_TICK;
import static com.nikitarybalko.wargamebroker.support.TestSimulationSettings.SECOND_TICK;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.BLUE_UNIT;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TickController.class)
@Import(SimulationStatusMapper.class)
@ActiveProfiles(TestProfiles.TEST)
@DisplayName("Web tests: TickController")
class TickControllerTest {

    private static final long ONGOING_TICK = 9;
    private static final long STATE_EPOCH = 3;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TickManager tickManager;

    @MockitoBean
    private StateCheckpointingService checkpointingService;

    @MockitoBean
    private SimulatorRegistry simulatorRegistry;


    @Test
    @DisplayName("Submit units fails with 401 when X-API-KEY header is missing")
    void submitUnitsWithoutApiKeyReturns401() throws Exception {
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(createUnitDto(BLUE_UNIT, Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(ERROR).value(HttpStatus.UNAUTHORIZED.getReasonPhrase()));
    }

    @Test
    @DisplayName("Submit units fails with 401 when X-API-KEY is invalid")
    void submitUnitsWithInvalidApiKeyReturns401() throws Exception {
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(createUnitDto(BLUE_UNIT, Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.INVALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Submit units succeeds with 200 when Blue submits Blue units")
    void submitUnitsBlueSuccess() throws Exception {
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.isTickComplete(FIRST_TICK)).thenReturn(false);

        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(createUnitDto(BLUE_UNIT, Side.BLUE, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TICK_NUMBER).value(FIRST_TICK))
                .andExpect(jsonPath(TICK_COMPLETE).value(false));

        verify(tickManager).submitUnits(eq(Side.BLUE), eq(FIRST_TICK), any());
    }

    @Test
    @DisplayName("Submit units fails with 403 when Blue attempts to submit Red units (anti-spoofing)")
    void submitUnitsBlueSpoofingRedFails() throws Exception {
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(createUnitDto(RED_UNIT, Side.RED, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Submit units fails with 400 when payload validation fails (negative view radius)")
    void submitUnitsValidationFailure() throws Exception {
        UnitDto invalidUnit = createUnitDto(BLUE_UNIT, Side.BLUE, 10.0, 20.0, -5.0, UnitStatus.HEALTHY);
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(invalidUnit))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR).value(HttpStatus.BAD_REQUEST.getReasonPhrase()));
    }

    @Test
    @DisplayName("Fog of War endpoint returns 200 with visible enemies for Blue")
    void getFogOfWarBlueSuccess() throws Exception {
        UnitState redSpotted = new UnitState(RED_UNIT, Side.RED, 12.0, 22.0, 40.0, UnitStatus.HEALTHY);
        when(tickManager.getVisibleEnemies(Side.BLUE, FIRST_TICK)).thenReturn(List.of(redSpotted));

        mockMvc.perform(get(TestEndpoints.fogOfWar(FIRST_TICK))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TICK_NUMBER).value(FIRST_TICK))
                .andExpect(jsonPath(SIDE).value(Side.BLUE.name()))
                .andExpect(jsonPath(VISIBLE_ENEMIES_LENGTH).value(1))
                .andExpect(jsonPath(FIRST_VISIBLE_ENEMY_ID).value(RED_UNIT));
    }

    @Test
    @DisplayName("Fog of War endpoint returns 403 when Blue requests Red intelligence")
    void getFogOfWarBlueForbiddenForRed() throws Exception {
        mockMvc.perform(get(TestEndpoints.fogOfWar(FIRST_TICK, Side.RED))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin API key can request Fog of War for either side")
    void getFogOfWarAdminAccess() throws Exception {
        when(tickManager.getVisibleEnemies(Side.RED, FIRST_TICK)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(TestEndpoints.fogOfWar(FIRST_TICK, Side.RED))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SIDE).value(Side.RED.name()));
    }

    @Test
    @DisplayName("Current tick status endpoint returns 200")
    void getCurrentTickStatus() throws Exception {
        when(tickManager.getCurrentTick()).thenReturn(ONGOING_TICK);
        when(tickManager.getStatus()).thenReturn(SimulationStatus.ACTIVE);
        when(tickManager.isTickComplete(ONGOING_TICK)).thenReturn(false);

        mockMvc.perform(get(TestEndpoints.CURRENT_TICK)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CURRENT_TICK).value(ONGOING_TICK))
                .andExpect(jsonPath(STATUS).value(SimulationStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("Fog of War endpoint returns 409 while tick is not complete")
    void getFogOfWarForIncompleteTickReturnsConflict() throws Exception {
        when(tickManager.getVisibleEnemies(Side.BLUE, SECOND_TICK)).thenThrow(new TickNotCompleteException(SECOND_TICK));

        mockMvc.perform(get(TestEndpoints.fogOfWar(SECOND_TICK))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Admin Fog of War request without side parameter returns 400")
    void getFogOfWarAdminWithoutSideReturnsBadRequest() throws Exception {
        mockMvc.perform(get(TestEndpoints.fogOfWar(FIRST_TICK))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Submit units returns 409 when simulator is not registered")
    void submitUnitsFromUnregisteredSimulatorReturnsConflict() throws Exception {
        doThrow(new SimulatorNotRegisteredException(Side.RED))
                .when(tickManager).submitUnits(eq(Side.RED), eq(FIRST_TICK), any());
        SubmitUnitsRequest request = SubmitUnitsRequest.builder()
                .tickNumber(FIRST_TICK)
                .units(List.of(createUnitDto(RED_UNIT, Side.RED, 10.0, 20.0, 50.0, UnitStatus.HEALTHY)))
                .build();

        mockMvc.perform(post(TestEndpoints.SUBMIT)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.RED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Own units endpoint returns latest units of the authenticated force")
    void getOwnUnitsForAuthenticatedSide() throws Exception {
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 40.0, 60.0, 50.0, UnitStatus.DAMAGED);
        when(tickManager.getCurrentTick()).thenReturn(ONGOING_TICK);
        when(tickManager.getEpoch()).thenReturn(STATE_EPOCH);
        when(tickManager.getLatestUnits(Side.BLUE)).thenReturn(List.of(blue));

        mockMvc.perform(get(TestEndpoints.OWN_UNITS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SIDE).value(Side.BLUE.name()))
                .andExpect(jsonPath(TICK_NUMBER).value(ONGOING_TICK))
                .andExpect(jsonPath(EPOCH).value(STATE_EPOCH))
                .andExpect(jsonPath(FIRST_UNIT_ID).value(BLUE_UNIT))
                .andExpect(jsonPath(FIRST_UNIT_STATUS).value(UnitStatus.DAMAGED.name()));
    }

    @Test
    @DisplayName("Own units endpoint returns 403 when Blue requests Red units")
    void getOwnUnitsForOpponentIsForbidden() throws Exception {
        mockMvc.perform(get(TestEndpoints.ownUnits(Side.RED))
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.BLUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Own units endpoint requires side parameter for admin requests")
    void getOwnUnitsAdminWithoutSideReturnsBadRequest() throws Exception {
        mockMvc.perform(get(TestEndpoints.OWN_UNITS)
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, TestApiKeys.ADMIN))
                .andExpect(status().isBadRequest());
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
