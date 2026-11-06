"use client";

import { useId, useMemo } from "react";
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
import type { Battlefield, Point, Side, Unit, UnitType } from "@/lib/types";

export type ViewMode = "ARBITER" | Side;

const BASE_WIDTH = 300;
const BASE_HEIGHT = 200;
const GRID_STEP = 25;
const EDGE_MARGIN = 10;
const COLLAR = 9;
const SCALE_BAR_LENGTH = 50;
const COLUMN_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";

interface Bounds {
  minX: number;
  minY: number;
  width: number;
  height: number;
}

interface Scene {
  units: Unit[];
  /** Units whose view zones and movement trails the current viewer is allowed to know about. */
  observers: Unit[];
  fogSide: Side | null;
  isSpotted: (unit: Unit) => boolean;
}

function snapDown(value: number) {
  return Math.floor(value / GRID_STEP) * GRID_STEP;
}

function snapUp(value: number) {
  return Math.ceil(value / GRID_STEP) * GRID_STEP;
}

function computeBounds(units: Unit[]): Bounds {
  const xs = units.map((unit) => unit.x);
  const ys = units.map((unit) => unit.y);
  const minX = Math.min(0, ...xs.map((x) => snapDown(x - EDGE_MARGIN)));
  const minY = Math.min(0, ...ys.map((y) => snapDown(y - EDGE_MARGIN)));
  const maxX = Math.max(BASE_WIDTH, ...xs.map((x) => snapUp(x + EDGE_MARGIN)));
  const maxY = Math.max(BASE_HEIGHT, ...ys.map((y) => snapUp(y + EDGE_MARGIN)));
  return { minX, minY, width: maxX - minX, height: maxY - minY };
}

function opponentOf(side: Side): Side {
  return side === "BLUE" ? "RED" : "BLUE";
}

function buildScene(battlefield: Battlefield, mode: ViewMode): Scene {
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

function gridLines(start: number, length: number) {
  return Array.from({ length: length / GRID_STEP + 1 }, (_, i) => start + i * GRID_STEP);
}

function MapSheet({ bounds, scale }: { bounds: Bounds; scale: number }) {
  const maxX = bounds.minX + bounds.width;
  const maxY = bounds.minY + bounds.height;
  const xs = gridLines(bounds.minX, bounds.width);
  const ys = gridLines(bounds.minY, bounds.height);
  const fontSize = 3.4 * scale;
  const collar = COLLAR * scale;
  const label = { fontSize, fill: MAP_COLORS.inkSoft, textAnchor: "middle" as const, dominantBaseline: "central" as const };

  return (
    <g>
      <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} fill={MAP_COLORS.sheet} />
      {xs.map((x) => (
        <line key={`gx${x}`} x1={x} y1={bounds.minY} x2={x} y2={maxY} stroke={MAP_COLORS.ruleSoft} vectorEffect="non-scaling-stroke" />
      ))}
      {ys.map((y) => (
        <line key={`gy${y}`} x1={bounds.minX} y1={y} x2={maxX} y2={y} stroke={MAP_COLORS.ruleSoft} vectorEffect="non-scaling-stroke" />
      ))}
      {xs.slice(0, -1).map((x, i) => (
        <g key={`cx${x}`}>
          <text x={x + GRID_STEP / 2} y={bounds.minY - collar / 2} {...label}>{COLUMN_LETTERS[i] ?? ""}</text>
          <text x={x + GRID_STEP / 2} y={maxY + collar / 2} {...label}>{COLUMN_LETTERS[i] ?? ""}</text>
        </g>
      ))}
      {ys.slice(0, -1).map((y, i) => (
        <g key={`cy${y}`}>
          <text x={bounds.minX - collar / 2} y={y + GRID_STEP / 2} {...label}>{i + 1}</text>
          <text x={maxX + collar / 2} y={y + GRID_STEP / 2} {...label}>{i + 1}</text>
        </g>
      ))}
      <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} fill="none"
            stroke={MAP_COLORS.ink} strokeWidth={1.5} vectorEffect="non-scaling-stroke" />
    </g>
  );
}

function ScaleBar({ bounds, scale }: { bounds: Bounds; scale: number }) {
  const half = SCALE_BAR_LENGTH / 2;
  const x = bounds.minX + 8 * scale;
  const y = bounds.minY + bounds.height - 9 * scale;
  const height = 1.6 * scale;
  const fontSize = 3 * scale;
  const segment = { height, stroke: MAP_COLORS.ink, vectorEffect: "non-scaling-stroke" as const };

  return (
    <g>
      <rect x={x} y={y} width={half} fill={MAP_COLORS.ink} {...segment} />
      <rect x={x + half} y={y} width={half} fill={MAP_COLORS.sheet} {...segment} />
      {[0, half, SCALE_BAR_LENGTH].map((offset) => (
        <text key={offset} x={x + offset} y={y - 1.5 * scale} fontSize={fontSize} textAnchor="middle" fill={MAP_COLORS.ink}>
          {offset}
        </text>
      ))}
      <text x={x + SCALE_BAR_LENGTH + 2.5 * scale} y={y + height} fontSize={fontSize} fill={MAP_COLORS.ink}>од.</text>
    </g>
  );
}

function NorthArrow({ bounds, scale }: { bounds: Bounds; scale: number }) {
  const x = bounds.minX + bounds.width - 9 * scale;
  const y = bounds.minY + 6 * scale;
  const s = 3 * scale;
  return (
    <g>
      <polygon points={`${x},${y} ${x + s * 0.6},${y + s * 1.8} ${x},${y + s * 1.4}`} fill={MAP_COLORS.ink} />
      <polygon points={`${x},${y} ${x - s * 0.6},${y + s * 1.8} ${x},${y + s * 1.4}`} fill={MAP_COLORS.sheet}
               stroke={MAP_COLORS.ink} strokeWidth={0.8} vectorEffect="non-scaling-stroke" />
      <text x={x} y={y + s * 2.9} fontSize={3 * scale} textAnchor="middle" fill={MAP_COLORS.ink}>Пн</text>
    </g>
  );
}

function ViewZone({ unit }: { unit: Unit }) {
  return (
    <circle cx={unit.x} cy={unit.y} r={unit.effectiveViewRadius} fill={SIDE_FILLS[unit.side]} fillOpacity={0.07}
            stroke={SIDE_COLORS[unit.side]} strokeOpacity={0.7} strokeDasharray="5 3" vectorEffect="non-scaling-stroke" />
  );
}

function Trail({ points, side }: { points: Point[]; side: Side }) {
  if (points.length < 2) {
    return null;
  }
  return (
    <polyline points={points.map((p) => `${p.x},${p.y}`).join(" ")} fill="none" stroke={SIDE_COLORS[side]}
              strokeOpacity={0.75} strokeWidth={1.4} strokeDasharray="1 3" strokeLinecap="round"
              vectorEffect="non-scaling-stroke" />
  );
}

function FogOverlay({ id, bounds, zones }: { id: string; bounds: Bounds; zones: Unit[] }) {
  const sheet = { x: bounds.minX, y: bounds.minY, width: bounds.width, height: bounds.height, mask: `url(#${id}-mask)` };
  return (
    <>
      <defs>
        <pattern id={`${id}-hatch`} width={3} height={3} patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
          <line x1={0} y1={0} x2={0} y2={3} stroke={MAP_COLORS.inkSoft} strokeWidth={0.5} strokeOpacity={0.55} />
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

interface SymbolBox {
  x: number;
  y: number;
  halfWidth: number;
  halfHeight: number;
}

/** Area inside the frame where the APP-6 function icon is drawn. */
function iconBox(unit: Unit, size: number): SymbolBox {
  return unit.side === "BLUE"
    ? { x: unit.x, y: unit.y, halfWidth: size, halfHeight: size * 0.7 }
    : { x: unit.x, y: unit.y, halfWidth: size / 2, halfHeight: size / 2 };
}

function frameBottom(unit: Unit, size: number) {
  return unit.y + (unit.side === "BLUE" ? size * 0.7 : size);
}

function UnitFrame({ unit, size, fill }: { unit: Unit; size: number; fill: string }) {
  const common = { fill, stroke: MAP_COLORS.ink, strokeWidth: 1.5, vectorEffect: "non-scaling-stroke" as const };
  if (unit.side === "BLUE") {
    return <rect x={unit.x - size} y={unit.y - size * 0.7} width={size * 2} height={size * 1.4} {...common} />;
  }
  const { x, y } = unit;
  return <polygon points={`${x},${y - size} ${x + size},${y} ${x},${y + size} ${x - size},${y}`} {...common} />;
}

function FunctionIcon({ type, box }: { type: UnitType; box: SymbolBox }) {
  const { x, y, halfWidth: w, halfHeight: h } = box;
  const line = { stroke: MAP_COLORS.ink, strokeWidth: 1.2, vectorEffect: "non-scaling-stroke" as const };
  const cross = <path d={`M${x - w},${y - h} L${x + w},${y + h} M${x + w},${y - h} L${x - w},${y + h}`} {...line} />;
  const track = <ellipse cx={x} cy={y} rx={w * 0.6} ry={h * 0.45} fill="none" {...line} />;

  switch (type) {
    case "ARMOR":
      return track;
    case "INFANTRY":
      return cross;
    case "MECHANIZED_INFANTRY":
      return (
        <>
          {cross}
          {track}
        </>
      );
    case "RECON":
      return <path d={`M${x - w},${y + h} L${x + w},${y - h}`} {...line} />;
    default:
      return null;
  }
}

/** MIL-STD-2525 operational condition amplifier: a bar under the frame for damaged and destroyed units. */
function ConditionBar({ unit, size }: { unit: Unit; size: number }) {
  if (unit.status === "HEALTHY") {
    return null;
  }
  const color = unit.status === "DAMAGED" ? MAP_COLORS.conditionDamaged : MAP_COLORS.conditionDestroyed;
  return (
    <rect x={unit.x - size} y={frameBottom(unit, size) + size * 0.3} width={size * 2} height={size * 0.45}
          fill={color} stroke={MAP_COLORS.ink} strokeWidth={0.8} vectorEffect="non-scaling-stroke" />
  );
}

function UnitMarker({ unit, size, spotted }: { unit: Unit; size: number; spotted: boolean }) {
  const destroyed = unit.status === "DESTROYED";
  const tooltip = [
    `${SIDE_LABELS[unit.side]} · ${unit.id}`,
    `Тип: ${UNIT_TYPE_LABELS[unit.unitType] ?? UNIT_TYPE_LABELS.UNKNOWN}`,
    `Стан: ${UNIT_STATUS_LABELS[unit.status]}`,
    `Координати: (${unit.x.toFixed(1)}, ${unit.y.toFixed(1)})`,
    `Радіус огляду: ${unit.effectiveViewRadius.toFixed(1)} з ${unit.viewRadius.toFixed(1)}`,
  ].join("\n");
  const labelY = frameBottom(unit, size) + size * (unit.status === "HEALTHY" ? 1.5 : 2.1);

  return (
    <g>
      <title>{tooltip}</title>
      {spotted && (
        <circle cx={unit.x} cy={unit.y} r={size * 2} fill="none" stroke={SIDE_COLORS[opponentOf(unit.side)]}
                strokeWidth={1.2} strokeDasharray="2 2" vectorEffect="non-scaling-stroke" />
      )}
      <UnitFrame unit={unit} size={size} fill={destroyed ? MAP_COLORS.destroyedFill : SIDE_FILLS[unit.side]} />
      <FunctionIcon type={unit.unitType} box={iconBox(unit, size)} />
      <ConditionBar unit={unit} size={size} />
      <text x={unit.x} y={labelY} fontSize={size * 0.95} textAnchor="middle" dominantBaseline="central"
            fill={destroyed ? MAP_COLORS.inkFaint : MAP_COLORS.ink}
            stroke={MAP_COLORS.sheet} strokeWidth={size * 0.4} paintOrder="stroke">
        {shortUnitName(unit)}
      </text>
    </g>
  );
}

interface BattlefieldMapProps {
  battlefield: Battlefield | null;
  trails: Trails;
  viewMode: ViewMode;
}

export function BattlefieldMap({ battlefield, trails, viewMode }: BattlefieldMapProps) {
  const fogId = `fog-${useId().replace(/:/g, "")}`;
  const scene = useMemo(() => (battlefield ? buildScene(battlefield, viewMode) : null), [battlefield, viewMode]);
  const allUnits = useMemo(
    () => (battlefield ? [...battlefield.blueUnits, ...battlefield.redUnits] : []),
    [battlefield],
  );
  const bounds = useMemo(() => computeBounds(allUnits), [allUnits]);
  const scale = bounds.width / BASE_WIDTH;
  const markerSize = 3.4 * scale;
  const collar = COLLAR * scale;
  const activeZones = scene?.observers.filter((unit) => unit.effectiveViewRadius > 0) ?? [];
  const viewBox = [bounds.minX - collar, bounds.minY - collar, bounds.width + collar * 2, bounds.height + collar * 2].join(" ");

  return (
    <div className="relative bg-paper">
      <svg viewBox={viewBox} className="block h-auto w-full" role="img" aria-label="Карта обстановки"
           style={{ fontFamily: "var(--font-plex-mono)" }}>
        <defs>
          <clipPath id={`${fogId}-sheet`}>
            <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} />
          </clipPath>
        </defs>
        <MapSheet bounds={bounds} scale={scale} />
        {scene && (
          <>
            <g clipPath={`url(#${fogId}-sheet)`}>
              {activeZones.map((unit) => <ViewZone key={unitKey(unit)} unit={unit} />)}
              {scene.observers.map((unit) => (
                <Trail key={unitKey(unit)} points={trails[unitKey(unit)] ?? []} side={unit.side} />
              ))}
              {scene.fogSide && <FogOverlay id={fogId} bounds={bounds} zones={activeZones} />}
            </g>
            {scene.units.map((unit) => (
              <UnitMarker key={unitKey(unit)} unit={unit} size={markerSize} spotted={scene.isSpotted(unit)} />
            ))}
          </>
        )}
        <ScaleBar bounds={bounds} scale={scale} />
        <NorthArrow bounds={bounds} scale={scale} />
      </svg>
      {allUnits.length === 0 && (
        <div className="absolute inset-0 flex items-center justify-center px-4">
          <div className="border border-ink bg-sheet px-4 py-2 text-center">
            <p className="font-heading text-sm font-semibold uppercase">Обстановка відсутня</p>
            <p className="text-xs text-ink-soft">Ініціалізуйте сесію та запустіть симулятори Blue і Red Force</p>
          </div>
        </div>
      )}
    </div>
  );
}
