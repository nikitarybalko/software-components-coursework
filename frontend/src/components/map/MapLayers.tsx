import type { Trails } from "@/hooks/useUnitTrails";
import {
  MAP_COLORS,
  SIDE_COLORS,
  SIDE_FILLS,
  SIDE_LABELS,
  UNIT_STATUS_LABELS,
  UNIT_TYPE_LABELS,
  shortUnitName,
  unitKey,
} from "@/lib/presentation";
import type { Battlefield, Point, Side, Unit } from "@/lib/types";
import { GRID_STEP, type Bounds } from "./geometry";
import { ConditionBar, UnitFrame, UnitPictogram, frameBottom, pictogramBox } from "./UnitSymbols";

export type ViewMode = "ARBITER" | Side;

const HATCH_SPACING = 3;

export interface Scene {
  units: Unit[];
  /** Units whose view zones and movement trails the current viewer is allowed to know about. */
  observers: Unit[];
  fogSide: Side | null;
  isSpotted: (unit: Unit) => boolean;
}

function opponentOf(side: Side): Side {
  return side === "BLUE" ? "RED" : "BLUE";
}

export function buildScene(battlefield: Battlefield, mode: ViewMode): Scene {
  const spottedBy: Record<Side, Set<string>> = {
    BLUE: new Set(battlefield.blueVisibleRed.map((unit) => unit.id)),
    RED: new Set(battlefield.redVisibleBlue.map((unit) => unit.id)),
  };
  const ownUnits: Record<Side, Unit[]> = { BLUE: battlefield.blueUnits, RED: battlefield.redUnits };

  if (mode === "ARBITER") {
    const units = [...battlefield.blueUnits, ...battlefield.redUnits];
    return {
      units,
      observers: units,
      fogSide: null,
      isSpotted: (unit) => spottedBy[opponentOf(unit.side)].has(unit.id),
    };
  }

  const contacts = mode === "BLUE" ? battlefield.blueVisibleRed : battlefield.redVisibleBlue;
  return {
    units: [...ownUnits[mode], ...contacts],
    observers: ownUnits[mode],
    fogSide: mode,
    isSpotted: (unit) => unit.side !== mode,
  };
}

function gridPositions(start: number, length: number) {
  return Array.from({ length: length / GRID_STEP + 1 }, (_, i) => start + i * GRID_STEP);
}

export function GridLines({ bounds }: { bounds: Bounds }) {
  const maxX = bounds.minX + bounds.width;
  const maxY = bounds.minY + bounds.height;
  const line = { stroke: MAP_COLORS.ruleSoft, vectorEffect: "non-scaling-stroke" as const };
  return (
    <g>
      {gridPositions(bounds.minX, bounds.width).map((x) => (
        <line key={`gx${x}`} x1={x} y1={bounds.minY} x2={x} y2={maxY} {...line} />
      ))}
      {gridPositions(bounds.minY, bounds.height).map((y) => (
        <line key={`gy${y}`} x1={bounds.minX} y1={y} x2={maxX} y2={y} {...line} />
      ))}
    </g>
  );
}

export function ViewZone({ unit }: { unit: Unit }) {
  return (
    <circle cx={unit.x} cy={unit.y} r={unit.effectiveViewRadius} fill={SIDE_FILLS[unit.side]} fillOpacity={0.07}
            stroke={SIDE_COLORS[unit.side]} strokeOpacity={0.7} strokeDasharray="5 3" vectorEffect="non-scaling-stroke" />
  );
}

export function Trail({ points, side }: { points: Point[]; side: Side }) {
  if (points.length < 2) {
    return null;
  }
  return (
    <polyline points={points.map((p) => `${p.x},${p.y}`).join(" ")} fill="none" stroke={SIDE_COLORS[side]}
              strokeOpacity={0.75} strokeWidth={1.4} strokeDasharray="1 3" strokeLinecap="round"
              vectorEffect="non-scaling-stroke" />
  );
}

export function FogOverlay({ id, bounds, zones, zoom }: { id: string; bounds: Bounds; zones: Unit[]; zoom: number }) {
  const sheet = { x: bounds.minX, y: bounds.minY, width: bounds.width, height: bounds.height, mask: `url(#${id}-mask)` };
  return (
    <>
      <defs>
        <pattern id={`${id}-hatch`} width={HATCH_SPACING} height={HATCH_SPACING} patternUnits="userSpaceOnUse"
                 patternTransform={`scale(${1 / zoom}) rotate(45)`}>
          <line x1={0} y1={0} x2={0} y2={HATCH_SPACING} stroke={MAP_COLORS.inkSoft} strokeWidth={0.5} strokeOpacity={0.55} />
        </pattern>
        <mask id={`${id}-mask`}>
          <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} fill="white" />
          {zones.map((unit) => (
            <circle key={unitKey(unit)} cx={unit.x} cy={unit.y} r={unit.effectiveViewRadius} fill="black" />
          ))}
        </mask>
      </defs>
      <rect {...sheet} fill={MAP_COLORS.sheet} fillOpacity={0.6} />
      <rect {...sheet} fill={`url(#${id}-hatch)`} />
    </>
  );
}

export function UnitMarker({ unit, size, spotted }: { unit: Unit; size: number; spotted: boolean }) {
  const destroyed = unit.status === "DESTROYED";
  const frameFill = destroyed ? MAP_COLORS.destroyedFill : SIDE_FILLS[unit.side];
  const tooltip = [
    `${SIDE_LABELS[unit.side]} · ${unit.id}`,
    `Тип: ${UNIT_TYPE_LABELS[unit.unitType] ?? UNIT_TYPE_LABELS.UNKNOWN}`,
    `Стан: ${UNIT_STATUS_LABELS[unit.status]}`,
    `Координати: (${unit.x.toFixed(1)}, ${unit.y.toFixed(1)})`,
    `Радіус огляду: ${unit.effectiveViewRadius.toFixed(1)} з ${unit.viewRadius.toFixed(1)}`,
  ].join("\n");
  const labelY = frameBottom(unit.side, unit.y, size) + size * (unit.status === "HEALTHY" ? 1.3 : 1.9);

  return (
    <g>
      <title>{tooltip}</title>
      {spotted && (
        <circle cx={unit.x} cy={unit.y} r={size * 2.1} fill="none" stroke={SIDE_COLORS[opponentOf(unit.side)]}
                strokeWidth={1.2} strokeDasharray="2 2" vectorEffect="non-scaling-stroke" />
      )}
      <UnitFrame side={unit.side} cx={unit.x} cy={unit.y} size={size} fill={frameFill} />
      <UnitPictogram type={unit.unitType} box={pictogramBox(unit.side, unit.x, unit.y, size)}
                     color={destroyed ? MAP_COLORS.inkFaint : MAP_COLORS.ink} hole={frameFill} />
      <ConditionBar unit={unit} size={size} />
      <text x={unit.x} y={labelY} fontSize={size * 0.9} textAnchor="middle" dominantBaseline="central"
            fill={destroyed ? MAP_COLORS.inkFaint : MAP_COLORS.ink}
            stroke={MAP_COLORS.sheet} strokeWidth={size * 0.35} paintOrder="stroke">
        {shortUnitName(unit)}
      </text>
    </g>
  );
}

export function SceneLayers({ scene, trails, bounds, zoom, markerSize, fogId }: {
  scene: Scene;
  trails: Trails;
  bounds: Bounds;
  zoom: number;
  markerSize: number;
  fogId: string;
}) {
  const zones = scene.observers.filter((unit) => unit.effectiveViewRadius > 0);
  return (
    <>
      {zones.map((unit) => <ViewZone key={unitKey(unit)} unit={unit} />)}
      {scene.observers.map((unit) => (
        <Trail key={unitKey(unit)} points={trails[unitKey(unit)] ?? []} side={unit.side} />
      ))}
      {scene.fogSide && <FogOverlay id={fogId} bounds={bounds} zones={zones} zoom={zoom} />}
      {scene.units.map((unit) => (
        <UnitMarker key={unitKey(unit)} unit={unit} size={markerSize} spotted={scene.isSpotted(unit)} />
      ))}
    </>
  );
}
