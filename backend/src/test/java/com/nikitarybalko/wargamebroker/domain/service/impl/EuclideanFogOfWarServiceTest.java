package com.nikitarybalko.wargamebroker.domain.service.impl;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.FogOfWarService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.nikitarybalko.wargamebroker.support.TestUnitIds.BLUE_UNIT;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.BLUE_UNIT_2;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT_2;
import static com.nikitarybalko.wargamebroker.support.TestUnitIds.RED_UNIT_3;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests: Fog of War Service (EuclideanFogOfWarService)")
class EuclideanFogOfWarServiceTest {

    private static final int UNITS_PER_SIDE = 500;
    private static final double UNIT_SPACING = 2.0;
    private static final double LINE_SEPARATION = 10.0;
    private static final double PERFORMANCE_VIEW_RADIUS = 25.0;
    private static final long MAX_CALCULATION_TIME_MS = 200;

    private FogOfWarService fogOfWarService;

    @BeforeEach
    void setUp() {
        fogOfWarService = new EuclideanFogOfWarService();
    }

    @Test
    @DisplayName("Unit spots enemy within view radius")
    void testVisibleWithinRadius() {
        UnitState observer = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 50.0, UnitStatus.HEALTHY);
        UnitState target = new UnitState(RED_UNIT, Side.RED, 30.0, 30.0, 10.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.isVisible(observer, target));

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(List.of(observer), List.of(target));
        assertEquals(1, visible.size());
        assertEquals(RED_UNIT, visible.get(0).getId());
    }

    @Test
    @DisplayName("Unit does NOT spot enemy outside view radius")
    void testNotVisibleOutsideRadius() {
        UnitState observer = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 20.0, UnitStatus.HEALTHY);
        UnitState target = new UnitState(RED_UNIT, Side.RED, 15.0, 20.0, 10.0, UnitStatus.HEALTHY);

        assertFalse(fogOfWarService.isVisible(observer, target));

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(List.of(observer), List.of(target));
        assertTrue(visible.isEmpty());
    }

    @Test
    @DisplayName("Enemy on the exact boundary of view radius is detected")
    void testVisibleOnExactBoundary() {
        UnitState observer = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState target = new UnitState(RED_UNIT, Side.RED, 6.0, 8.0, 10.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.isVisible(observer, target));
    }

    @Test
    @DisplayName("Dynamic visibility: DAMAGED unit loses 50% of view radius")
    void testDynamicVisibilityForDamagedUnit() {
        UnitState healthyObserver = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 100.0, UnitStatus.HEALTHY);
        UnitState targetAt70 = new UnitState(RED_UNIT, Side.RED, 70.0, 0.0, 10.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.isVisible(healthyObserver, targetAt70));

        UnitState damagedObserver = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 100.0, UnitStatus.DAMAGED);
        assertFalse(fogOfWarService.isVisible(damagedObserver, targetAt70));

        UnitState targetAt40 = new UnitState(RED_UNIT_2, Side.RED, 40.0, 0.0, 10.0, UnitStatus.HEALTHY);
        assertTrue(fogOfWarService.isVisible(damagedObserver, targetAt40));
    }

    @Test
    @DisplayName("DESTROYED observer has no sight and detects nobody")
    void testDestroyedObserverHasNoSight() {
        UnitState destroyedObserver = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 100.0, UnitStatus.DESTROYED);
        UnitState target = new UnitState(RED_UNIT, Side.RED, 1.0, 1.0, 10.0, UnitStatus.HEALTHY);

        assertFalse(fogOfWarService.isVisible(destroyedObserver, target));

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(
                List.of(destroyedObserver), List.of(target));
        assertTrue(visible.isEmpty());
    }

    @Test
    @DisplayName("Multiple allies detection and deduplication of spotted enemies")
    void testMultipleAlliesDetectionAndDeduplication() {
        UnitState blue1 = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 30.0, UnitStatus.HEALTHY);
        UnitState blue2 = new UnitState(BLUE_UNIT_2, Side.BLUE, 50.0, 0.0, 30.0, UnitStatus.HEALTHY);

        UnitState r1 = new UnitState(RED_UNIT, Side.RED, 25.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState r2 = new UnitState(RED_UNIT_2, Side.RED, 70.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState r3 = new UnitState(RED_UNIT_3, Side.RED, 200.0, 0.0, 10.0, UnitStatus.HEALTHY);

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(
                List.of(blue1, blue2), List.of(r1, r2, r3));

        assertEquals(2, visible.size());
        assertTrue(visible.stream().anyMatch(u -> u.getId().equals(RED_UNIT)));
        assertTrue(visible.stream().anyMatch(u -> u.getId().equals(RED_UNIT_2)));
        assertFalse(visible.stream().anyMatch(u -> u.getId().equals(RED_UNIT_3)));
    }

    @Test
    @DisplayName("Edge cases: empty allies or enemies lists are handled safely")
    void testEmptyInputsHandledSafely() {
        UnitState blue = new UnitState(BLUE_UNIT, Side.BLUE, 0.0, 0.0, 50.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.calculateVisibleEnemies(Collections.emptyList(), List.of(blue)).isEmpty());
        assertTrue(fogOfWarService.calculateVisibleEnemies(List.of(blue), Collections.emptyList()).isEmpty());
        assertTrue(fogOfWarService.calculateVisibleEnemies(null, null).isEmpty());
    }

    @Test
    @DisplayName("Performance test: 500 allies vs 500 enemies executes within O(N*M)")
    void testPerformanceWith500Units() {
        List<UnitState> blues = IntStream.range(0, UNITS_PER_SIDE)
                .mapToObj(i -> new UnitState(BLUE_UNIT + i, Side.BLUE, i * UNIT_SPACING, 0.0,
                        PERFORMANCE_VIEW_RADIUS, UnitStatus.HEALTHY))
                .toList();
        List<UnitState> reds = IntStream.range(0, UNITS_PER_SIDE)
                .mapToObj(i -> new UnitState(RED_UNIT + i, Side.RED, i * UNIT_SPACING, LINE_SEPARATION,
                        PERFORMANCE_VIEW_RADIUS, UnitStatus.HEALTHY))
                .toList();

        long startTime = System.currentTimeMillis();
        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(blues, reds);
        long duration = System.currentTimeMillis() - startTime;

        assertFalse(visible.isEmpty());
        assertTrue(duration < MAX_CALCULATION_TIME_MS, "Calculation for " + UNITS_PER_SIDE + "x" + UNITS_PER_SIDE
                + " units must finish within " + MAX_CALCULATION_TIME_MS + "ms, actual: " + duration + "ms");
    }
}
