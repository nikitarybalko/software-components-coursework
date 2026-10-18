package com.nikitarybalko.wargamebroker.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests: UnitState model")
class UnitStateTest {

    @Test
    @DisplayName("Effective view radius for HEALTHY unit equals base radius")
    void testEffectiveViewRadiusHealthy() {
        UnitState unit = new UnitState("u1", Side.BLUE, 10.0, 20.0, 100.0, UnitStatus.HEALTHY);
        assertEquals(100.0, unit.getEffectiveViewRadius(), 1e-9);
        assertTrue(unit.isOperational());
    }

    @Test
    @DisplayName("Effective view radius for DAMAGED unit is halved (50%)")
    void testEffectiveViewRadiusDamaged() {
        UnitState unit = new UnitState("u2", Side.RED, 0.0, 0.0, 80.0, UnitStatus.DAMAGED);
        assertEquals(40.0, unit.getEffectiveViewRadius(), 1e-9);
        assertTrue(unit.isOperational());
    }

    @Test
    @DisplayName("Effective view radius for DESTROYED unit is zero")
    void testEffectiveViewRadiusDestroyed() {
        UnitState unit = new UnitState("u3", Side.BLUE, 5.0, 5.0, 50.0, UnitStatus.DESTROYED);
        assertEquals(0.0, unit.getEffectiveViewRadius(), 1e-9);
        assertFalse(unit.isOperational());
    }

    @Test
    @DisplayName("Euclidean distance calculation between points (3, 4) and (0, 0)")
    void testDistanceToEuclidean() {
        UnitState u1 = new UnitState("u1", Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY);
        UnitState u2 = new UnitState("u2", Side.RED, 3.0, 4.0, 10.0, UnitStatus.HEALTHY);

        assertEquals(5.0, u1.distanceTo(u2), 1e-9);
        assertEquals(5.0, u2.distanceTo(u1), 1e-9);
    }

    @Test
    @DisplayName("Euclidean distance calculation with negative coordinates")
    void testDistanceToNegativeCoordinates() {
        UnitState u1 = new UnitState("u1", Side.BLUE, -10.0, -20.0, 10.0, UnitStatus.HEALTHY);
        UnitState u2 = new UnitState("u2", Side.RED, 20.0, 20.0, 10.0, UnitStatus.HEALTHY);

        assertEquals(50.0, u1.distanceTo(u2), 1e-9);
    }

    @Test
    @DisplayName("Negative base view radius is safely clamped to 0.0")
    void testNegativeViewRadiusHandled() {
        UnitState unit = new UnitState("u1", Side.BLUE, 0.0, 0.0, -15.0, UnitStatus.HEALTHY);
        assertEquals(0.0, unit.getViewRadius(), 1e-9);
        assertEquals(0.0, unit.getEffectiveViewRadius(), 1e-9);
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when required fields are missing")
    void testNullValidationInConstructor() {
        assertThrows(NullPointerException.class, () ->
                new UnitState(null, Side.BLUE, 0.0, 0.0, 10.0, UnitStatus.HEALTHY));

        assertThrows(NullPointerException.class, () ->
                new UnitState("u1", null, 0.0, 0.0, 10.0, UnitStatus.HEALTHY));
    }
}
