package com.nikitarybalko.wargamebroker.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * JsonPath expressions for MockMvc assertions and field names for reading JSON trees.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestJsonPaths {

    public static final String ARRAY_LENGTH = "$.length()";
    public static final String ERROR = "$.error";

    public static final String STATUS = "$.status";
    public static final String CURRENT_TICK = "$.currentTick";
    public static final String TICK_NUMBER = "$.tickNumber";
    public static final String TICK_COMPLETE = "$.tickComplete";
    public static final String SIDE = "$.side";
    public static final String EPOCH = "$.epoch";
    public static final String REGISTERED_SIDES_LENGTH = "$.registeredSides.length()";
    public static final String SUBMITTED_SIDES_LENGTH = "$.submittedSides.length()";
    public static final String FIRST_SUBMITTED_SIDE = "$.submittedSides[0]";

    public static final String VISIBLE_ENEMIES_LENGTH = "$.visibleEnemies.length()";
    public static final String FIRST_VISIBLE_ENEMY_ID = "$.visibleEnemies[0].id";

    public static final String UNITS_LENGTH = "$.units.length()";
    public static final String FIRST_UNIT_ID = "$.units[0].id";
    public static final String FIRST_UNIT_STATUS = "$.units[0].status";
    public static final String FIRST_UNIT_X = "$.units[0].x";

    public static final String BLUE_UNITS_LENGTH = "$.blueUnits.length()";
    public static final String RED_UNITS_LENGTH = "$.redUnits.length()";
    public static final String BLUE_VISIBLE_RED_LENGTH = "$.blueVisibleRed.length()";
    public static final String RED_VISIBLE_BLUE_LENGTH = "$.redVisibleBlue.length()";
    public static final String FIRST_BLUE_UNIT_VIEW_RADIUS = "$.blueUnits[0].viewRadius";
    public static final String FIRST_BLUE_UNIT_TYPE = "$.blueUnits[0].unitType";
    public static final String FIRST_RED_UNIT_TYPE = "$.redUnits[0].unitType";
    public static final String FIRST_BLUE_UNIT_EFFECTIVE_VIEW_RADIUS = "$.blueUnits[0].effectiveViewRadius";

    public static final String FIRST_ITEM_TICK_NUMBER = "$[0].tickNumber";
    public static final String SECOND_ITEM_TICK_NUMBER = "$[1].tickNumber";
    public static final String FIRST_ITEM_TYPE = "$[0].type";
    public static final String FIRST_ITEM_SIDE = "$[0].side";

    public static final String FIELD_ID = "id";
    public static final String FIELD_EPOCH = "epoch";
    public static final String FIELD_X = "x";
    public static final String FIELD_UNITS = "units";
    public static final String FIELD_UNIT_TYPE = "unitType";
    public static final String FIELD_BLUE_UNITS = "blueUnits";
    public static final String FIELD_RED_UNITS = "redUnits";
}
