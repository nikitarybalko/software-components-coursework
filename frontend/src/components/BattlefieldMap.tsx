"use client";

import { useId, useMemo } from "react";
import type { Trails } from "@/hooks/useUnitTrails";
import {
  DAMAGED_COLOR,
  DESTROYED_COLOR,
  SIDE_COLORS,
  SIDE_LABELS,
  UNIT_STATUS_LABELS,
  UNIT_TYPE_LABELS,
  shortUnitName,
  unitKey,
} from "@/lib/presentation";
import type { Battlefield, Side, Unit, UnitType } from "@/lib/types";

export type ViewMode = "ARBITER" | Side;

const BASE_WIDTH = 300;
const BASE_HEIGHT = 200;
const GRID_STEP = 25;
const LABEL_STEP = 50;
const EDGE_MARGIN = 10;

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

function Grid({ bounds, scale }: { bounds: Bounds; scale: number }) {
  const maxX = bounds.minX + bounds.width;
  const maxY = bounds.minY + bounds.height;
  const xs = Array.from({ length: bounds.width / GRID_STEP + 1 }, (_, i) => bounds.minX + i * GRID_STEP);
  const ys = Array.from({ length: bounds.height / GRID_STEP + 1 }, (_, i) => bounds.minY + i * GRID_STEP);
  const fontSize = 3.2 * scale;

  return (
    <g>
      {xs.map((x) => (
        <line key={`x${x}`} x1={x} y1={bounds.minY} x2={x} y2={maxY}
              stroke={x % LABEL_STEP === 0 ? "#1e293b" : "#111a2e"} vectorEffect="non-scaling-stroke" />
      ))}
      {ys.map((y) => (
        <line key={`y${y}`} x1={bounds.minX} y1={y} x2={maxX} y2={y}
              stroke={y % LABEL_STEP === 0 ? "#1e293b" : "#111a2e"} vectorEffect="non-scaling-stroke" />
      ))}
      {xs.filter((x) => x % LABEL_STEP === 0 && x !== bounds.minX).map((x) => (
        <text key={`lx${x}`} x={x + scale} y={bounds.minY + fontSize * 1.2} fontSize={fontSize} fill="#475569">{x}</text>
      ))}
      {ys.filter((y) => y % LABEL_STEP === 0 && y !== bounds.minY).map((y) => (
        <text key={`ly${y}`} x={bounds.minX + scale} y={y - scale} fontSize={fontSize} fill="#475569">{y}</text>
      ))}
    </g>
  );
}

function ViewZone({ unit }: { unit: Unit }) {
  const color = SIDE_COLORS[unit.side];
  return (
    <circle cx={unit.x} cy={unit.y} r={unit.effectiveViewRadius} fill={color} fillOpacity={0.07}
            stroke={color} strokeOpacity={0.45} strokeDasharray="4 4" vectorEffect="non-scaling-stroke" />
  );
}

function Trail({ points, side }: { points: { x: number; y: number }[]; side: Side }) {
  if (points.length < 2) {
    return null;
  }
  return (
    <polyline points={points.map((p) => `${p.x},${p.y}`).join(" ")} fill="none" stroke={SIDE_COLORS[side]}
              strokeOpacity={0.3} strokeWidth={1.5} strokeLinejoin="round" vectorEffect="non-scaling-stroke" />
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

function UnitFrame({ unit, size, fill, stroke, dashed }: {
  unit: Unit;
  size: number;
  fill: string;
  stroke: string;
  dashed: boolean;
}) {
  const common = {
    fill,
    fillOpacity: 0.3,
    stroke,
    strokeWidth: 2,
    strokeDasharray: dashed ? "3 2" : undefined,
    vectorEffect: "non-scaling-stroke" as const,
  };
  if (unit.side === "BLUE") {
    return <rect x={unit.x - size} y={unit.y - size * 0.7} width={size * 2} height={size * 1.4} {...common} />;
  }
  const { x, y } = unit;
  return <polygon points={`${x},${y - size} ${x + size},${y} ${x},${y + size} ${x - size},${y}`} {...common} />;
}

function FunctionIcon({ type, box, stroke }: { type: UnitType; box: SymbolBox; stroke: string }) {
  const { x, y, halfWidth: w, halfHeight: h } = box;
  const line = { stroke, strokeWidth: 1.5, vectorEffect: "non-scaling-stroke" as const };
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

function DestroyedMark({ unit, size }: { unit: Unit; size: number }) {
  const cx = unit.x + size * 1.15;
  const cy = unit.y - size * 1.05;
  const r = size * 0.45;
  return (
    <g>
      <circle cx={cx} cy={cy} r={r} fill="#0b1222" stroke={DESTROYED_COLOR} vectorEffect="non-scaling-stroke" />
      <path d={`M${cx - r * 0.5},${cy - r * 0.5} L${cx + r * 0.5},${cy + r * 0.5} M${cx + r * 0.5},${cy - r * 0.5} L${cx - r * 0.5},${cy + r * 0.5}`}
            stroke="#e2e8f0" strokeWidth={1.2} vectorEffect="non-scaling-stroke" />
    </g>
  );
}

function UnitMarker({ unit, size, spotted }: { unit: Unit; size: number; spotted: boolean }) {
  const destroyed = unit.status === "DESTROYED";
  const color = destroyed ? DESTROYED_COLOR : SIDE_COLORS[unit.side];
  const stroke = unit.status === "DAMAGED" ? DAMAGED_COLOR : color;
  const tooltip = [
    `${SIDE_LABELS[unit.side]} · ${unit.id}`,
    `Тип: ${UNIT_TYPE_LABELS[unit.unitType] ?? UNIT_TYPE_LABELS.UNKNOWN}`,
    `Стан: ${UNIT_STATUS_LABELS[unit.status]}`,
    `Координати: (${unit.x.toFixed(1)}, ${unit.y.toFixed(1)})`,
    `Радіус огляду: ${unit.effectiveViewRadius.toFixed(1)} з ${unit.viewRadius.toFixed(1)}`,
  ].join("\n");

  return (
    <g opacity={destroyed ? 0.75 : 1}>
      <title>{tooltip}</title>
      {spotted && (
        <circle cx={unit.x} cy={unit.y} r={size * 1.9} fill="none" stroke={SIDE_COLORS[opponentOf(unit.side)]}
                strokeWidth={1.5} strokeDasharray="2 3" vectorEffect="non-scaling-stroke" className="animate-pulse" />
      )}
      <UnitFrame unit={unit} size={size} fill={color} stroke={stroke} dashed={destroyed} />
      <FunctionIcon type={unit.unitType} box={iconBox(unit, size)} stroke={destroyed ? "#94a3b8" : "#e2e8f0"} />
      {destroyed && <DestroyedMark unit={unit} size={size} />}
      <text x={unit.x} y={unit.y + size * 2.5} fontSize={size * 1.1} textAnchor="middle" fill="#cbd5e1"
            stroke="#020617" strokeWidth={size * 0.35} paintOrder="stroke">
        {shortUnitName(unit)}
      </text>
    </g>
  );
}

function FogOverlay({ maskId, bounds, zones }: { maskId: string; bounds: Bounds; zones: Unit[] }) {
  return (
    <>
      <defs>
        <mask id={maskId}>
          <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} fill="white" />
          {zones.map((unit) => (
            <circle key={unitKey(unit)} cx={unit.x} cy={unit.y} r={unit.effectiveViewRadius} fill="black" />
          ))}
        </mask>
      </defs>
      <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height}
            fill="#020617" fillOpacity={0.78} mask={`url(#${maskId})`} />
    </>
  );
}

interface BattlefieldMapProps {
  battlefield: Battlefield | null;
  trails: Trails;
  viewMode: ViewMode;
}

export function BattlefieldMap({ battlefield, trails, viewMode }: BattlefieldMapProps) {
  const maskId = `fog-${useId().replace(/:/g, "")}`;
  const scene = useMemo(() => (battlefield ? buildScene(battlefield, viewMode) : null), [battlefield, viewMode]);
  const allUnits = useMemo(
    () => (battlefield ? [...battlefield.blueUnits, ...battlefield.redUnits] : []),
    [battlefield],
  );
  const bounds = useMemo(() => computeBounds(allUnits), [allUnits]);
  const scale = bounds.width / BASE_WIDTH;
  const markerSize = 3.2 * scale;
  const activeZones = scene?.observers.filter((unit) => unit.effectiveViewRadius > 0) ?? [];

  return (
    <div className="relative overflow-hidden rounded-lg border border-slate-800 bg-[#0b1222]">
      <svg viewBox={`${bounds.minX} ${bounds.minY} ${bounds.width} ${bounds.height}`} className="block h-auto w-full"
           role="img" aria-label="Карта поля бою">
        <Grid bounds={bounds} scale={scale} />
        {scene && (
          <>
            {scene.observers.map((unit) => (
              <Trail key={unitKey(unit)} points={trails[unitKey(unit)] ?? []} side={unit.side} />
            ))}
            {activeZones.map((unit) => <ViewZone key={unitKey(unit)} unit={unit} />)}
            {scene.fogSide && <FogOverlay maskId={maskId} bounds={bounds} zones={activeZones} />}
            {scene.units.map((unit) => (
              <UnitMarker key={unitKey(unit)} unit={unit} size={markerSize} spotted={scene.isSpotted(unit)} />
            ))}
          </>
        )}
      </svg>
      {allUnits.length === 0 && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 px-4 text-center">
          <p className="text-sm font-medium text-slate-300">Немає даних про юніти</p>
          <p className="text-xs text-slate-500">Ініціалізуйте симуляцію та запустіть симулятори Blue і Red Force</p>
        </div>
      )}
    </div>
  );
}
