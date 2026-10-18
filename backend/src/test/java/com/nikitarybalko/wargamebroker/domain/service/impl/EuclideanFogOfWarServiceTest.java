package com.nikitarybalko.wargamebroker.domain.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.UnitState;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.service.FogOfWarService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests: Fog of War Service (EuclideanFogOfWarService)")
class EuclideanFogOfWarServiceTest {

    private FogOfWarService fogOfWarService;

    @BeforeEach
    void setUp() {
        fogOfWarService = new EuclideanFogOfWarService();
    }

    @Test
    @DisplayName("Unit spots enemy within view radius")
    void testVisibleWithinRadius() {
        UnitState observer = new UnitState("b1", Side.BLUE, 0.0, 0.0, 50.0, UnitStatus.HEALTHY);
        UnitState target = new UnitState("r1", Side.RED, 30.0, 30.0, 10.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.isVisible(observer, target));

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(List.of(observer), List.of(target));
        assertEquals(1, visible.size());
        assertEquals("r1", visible.get(0).getId());
    }

    @Test
    @DisplayName("Unit does NOT spot enemy outside view radius")
    void testNotVisibleOutsideRadius() {
        UnitState observer = new UnitState("b1", Side.BLUE, 0.0, 0.0, 20.0, UnitStatus.HEALTHY);
        UnitState target = new UnitState("r1", Side.RED, 15.0, 20.0, 10.0, UnitStatus.HEALTHY);

        assertFalse(fogOfWarService.isVisible(observer, target));

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(List.of(observer), List.of(target));
        assertTrue(visible.isEmpty());
    }

    @Test
    @DisplayName("Enemy on the exact boundary of view radius is detected")
    void testVisibleOnExactBoundary() {
        UnitState observer = new UnitState("b1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState target = new UnitState("r1", Side.RED, 6.0, 8.0, 10.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.isVisible(observer, target));
    }

    @Test
    @DisplayName("Dynamic visibility: DAMAGED unit loses 50% of view radius")
    void testDynamicVisibilityForDamagedUnit() {
        UnitState healthyObserver = new UnitState("b1", Side.BLUE, 0.0, 0.0, 100.0, UnitStatus.HEALTHY);
        UnitState targetAt70 = new UnitState("r1", Side.RED, 70.0, 0.0, 10.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.isVisible(healthyObserver, targetAt70));

        UnitState damagedObserver = new UnitState("b1", Side.BLUE, 0.0, 0.0, 100.0, UnitStatus.DAMAGED);
        assertFalse(fogOfWarService.isVisible(damagedObserver, targetAt70));

        UnitState targetAt40 = new UnitState("r2", Side.RED, 40.0, 0.0, 10.0, UnitStatus.HEALTHY);
        assertTrue(fogOfWarService.isVisible(damagedObserver, targetAt40));
    }

    @Test
    @DisplayName("DESTROYED observer has no sight and detects nobody")
    void testDestroyedObserverHasNoSight() {
        UnitState destroyedObserver = new UnitState("b1", Side.BLUE, 0.0, 0.0, 100.0, UnitStatus.DESTROYED);
        UnitState target = new UnitState("r1", Side.RED, 1.0, 1.0, 10.0, UnitStatus.HEALTHY);

        assertFalse(fogOfWarService.isVisible(destroyedObserver, target));

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(
                List.of(destroyedObserver), List.of(target));
        assertTrue(visible.isEmpty());
    }

    @Test
    @DisplayName("Multiple allies detection and deduplication of spotted enemies")
    void testMultipleAlliesDetectionAndDeduplication() {
        UnitState blue1 = new UnitState("b1", Side.BLUE, 0.0, 0.0, 30.0, UnitStatus.HEALTHY);
        UnitState blue2 = new UnitState("b2", Side.BLUE, 50.0, 0.0, 30.0, UnitStatus.HEALTHY);

        UnitState r1 = new UnitState("r1", Side.RED, 25.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState r2 = new UnitState("r2", Side.RED, 70.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState r3 = new UnitState("r3", Side.RED, 200.0, 0.0, 10.0, UnitStatus.HEALTHY);

        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(
                List.of(blue1, blue2), List.of(r1, r2, r3));

        assertEquals(2, visible.size());
        assertTrue(visible.stream().anyMatch(u -> u.getId().equals("r1")));
        assertTrue(visible.stream().anyMatch(u -> u.getId().equals("r2")));
        assertFalse(visible.stream().anyMatch(u -> u.getId().equals("r3")));
    }

    @Test
    @DisplayName("Edge cases: empty allies or enemies lists are handled safely")
    void testEmptyInputsHandledSafely() {
        UnitState blue = new UnitState("b1", Side.BLUE, 0.0, 0.0, 50.0, UnitStatus.HEALTHY);

        assertTrue(fogOfWarService.calculateVisibleEnemies(Collections.emptyList(), List.of(blue)).isEmpty());
        assertTrue(fogOfWarService.calculateVisibleEnemies(List.of(blue), Collections.emptyList()).isEmpty());
        assertTrue(fogOfWarService.calculateVisibleEnemies(null, null).isEmpty());
    }

    @Test
    @DisplayName("Performance test: 500 allies vs 500 enemies executes within O(N*M)")
    void testPerformanceWith500Units() {
        List<UnitState> blues = new ArrayList<>(500);
        List<UnitState> reds = new ArrayList<>(500);

        for (int i = 0; i < 500; i++) {
            blues.add(new UnitState("b" + i, Side.BLUE, i * 2.0, 0.0, 25.0, UnitStatus.HEALTHY));
            reds.add(new UnitState("r" + i, Side.RED, i * 2.0, 10.0, 25.0, UnitStatus.HEALTHY));
        }

        long startTime = System.currentTimeMillis();
        List<UnitState> visible = fogOfWarService.calculateVisibleEnemies(blues, reds);
        long duration = System.currentTimeMillis() - startTime;

        assertFalse(visible.isEmpty());
        assertTrue(duration < 200, "Calculation for O(500*500) must execute within 200ms, actual: " + duration + "ms");
    }
}
