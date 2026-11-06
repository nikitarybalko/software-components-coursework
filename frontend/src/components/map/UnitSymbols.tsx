import type { ReactNode } from "react";
import { MAP_COLORS } from "@/lib/presentation";
import type { Side, Unit, UnitType } from "@/lib/types";

/** Pictograms are drawn in a 24 x 16 box and scaled into the frame. */
const GLYPH_WIDTH = 24;
const GLYPH_HEIGHT = 16;
const GLYPH_FILL_RATIO = 0.95;

/** Hostile diamond is drawn larger than the friendly rectangle so both hold a pictogram of similar size. */
const HOSTILE_FRAME_SCALE = 1.35;
const FRIEND_FRAME_ASPECT = 0.7;

type Glyph = (color: string, hole: string) => ReactNode;

const GLYPHS: Record<UnitType, Glyph> = {
  ARMOR: (color, hole) => (
    <>
      <rect x={1.5} y={10} width={21} height={5} rx={2.5} fill={color} />
      {[5, 9.7, 14.3, 19].map((cx) => <circle key={cx} cx={cx} cy={12.5} r={1.3} fill={hole} />)}
      <polygon points="3,10 5.5,7.2 19,7.2 21.5,10" fill={color} />
      <rect x={8} y={3.8} width={7.5} height={3.6} rx={1.2} fill={color} />
      <rect x={15} y={4.9} width={8.5} height={1.2} fill={color} />
    </>
  ),
  MECHANIZED_INFANTRY: (color, hole) => (
    <>
      <polygon points="1.5,11.5 1.5,7 4,4.8 17,4.8 22.5,8.2 22.5,11.5" fill={color} />
      <rect x={8} y={2.6} width={4.5} height={2.4} rx={0.6} fill={color} />
      <rect x={12.2} y={3.2} width={6} height={0.9} fill={color} />
      {[5, 9.5, 14.5, 19].map((cx) => (
        <g key={cx}>
          <circle cx={cx} cy={12.6} r={2.2} fill={color} stroke={hole} strokeWidth={0.5} />
          <circle cx={cx} cy={12.6} r={0.8} fill={hole} />
        </g>
      ))}
    </>
  ),
  INFANTRY: (color) => (
    <>
      <line x1={6.5} y1={12.5} x2={17.8} y2={4.8} stroke={color} strokeWidth={1.1} strokeLinecap="round" />
      <path d="M9.5,3.7 A2.5,2.3 0 0 1 14.5,3.7 L15,4 L9,4 Z" fill={color} />
      <circle cx={12} cy={4.7} r={1.5} fill={color} />
      <path d="M10,6.5 L14,6.5 L14.6,11 L9.4,11 Z" fill={color} />
      <path d="M9.6,11 L11.7,11 L11.2,15.5 L9.4,15.5 Z" fill={color} />
      <path d="M12.3,11 L14.4,11 L14.7,15.5 L12.8,15.5 Z" fill={color} />
    </>
  ),
  RECON: (color, hole) => (
    <>
      <rect x={6.3} y={2.6} width={4.2} height={3.6} rx={0.8} fill={color} />
      <rect x={13.5} y={2.6} width={4.2} height={3.6} rx={0.8} fill={color} />
      <rect x={10.2} y={5.2} width={3.6} height={2.6} fill={color} />
      <circle cx={8.4} cy={10} r={4} fill={color} />
      <circle cx={15.6} cy={10} r={4} fill={color} />
      <circle cx={8.4} cy={10} r={2} fill={hole} />
      <circle cx={15.6} cy={10} r={2} fill={hole} />
    </>
  ),
  UNKNOWN: (color) => (
    <text x={12} y={8.5} fontSize={12} fontWeight={600} textAnchor="middle" dominantBaseline="central" fill={color}>?</text>
  ),
};

export interface SymbolBox {
  cx: number;
  cy: number;
  halfWidth: number;
  halfHeight: number;
}

export function UnitPictogram({ type, box, color, hole }: { type: UnitType; box: SymbolBox; color: string; hole: string }) {
  const scale = Math.min((box.halfWidth * 2) / GLYPH_WIDTH, (box.halfHeight * 2) / GLYPH_HEIGHT) * GLYPH_FILL_RATIO;
  const glyph = GLYPHS[type] ?? GLYPHS.UNKNOWN;
  return (
    <g transform={`translate(${box.cx - (GLYPH_WIDTH / 2) * scale} ${box.cy - (GLYPH_HEIGHT / 2) * scale}) scale(${scale})`}>
      {glyph(color, hole)}
    </g>
  );
}

function hostileHalfDiagonal(size: number) {
  return size * HOSTILE_FRAME_SCALE;
}

/** Largest pictogram box with the glyph's aspect ratio that fits the frame. */
export function pictogramBox(side: Side, cx: number, cy: number, size: number): SymbolBox {
  if (side === "BLUE") {
    return { cx, cy, halfWidth: size, halfHeight: size * FRIEND_FRAME_ASPECT };
  }
  const diagonal = hostileHalfDiagonal(size);
  return { cx, cy, halfWidth: diagonal * 0.6, halfHeight: diagonal * 0.4 };
}

export function frameBottom(side: Side, cy: number, size: number) {
  return cy + (side === "BLUE" ? size * FRIEND_FRAME_ASPECT : hostileHalfDiagonal(size));
}

export function UnitFrame({ side, cx, cy, size, fill }: { side: Side; cx: number; cy: number; size: number; fill: string }) {
  const common = { fill, stroke: MAP_COLORS.ink, strokeWidth: 1.5, vectorEffect: "non-scaling-stroke" as const };
  if (side === "BLUE") {
    const halfHeight = size * FRIEND_FRAME_ASPECT;
    return <rect x={cx - size} y={cy - halfHeight} width={size * 2} height={halfHeight * 2} {...common} />;
  }
  const d = hostileHalfDiagonal(size);
  return <polygon points={`${cx},${cy - d} ${cx + d},${cy} ${cx},${cy + d} ${cx - d},${cy}`} {...common} />;
}

/** MIL-STD-2525 operational condition amplifier: a bar under the frame for damaged and destroyed units. */
export function ConditionBar({ unit, size }: { unit: Unit; size: number }) {
  if (unit.status === "HEALTHY") {
    return null;
  }
  const color = unit.status === "DAMAGED" ? MAP_COLORS.conditionDamaged : MAP_COLORS.conditionDestroyed;
  return (
    <rect x={unit.x - size} y={frameBottom(unit.side, unit.y, size) + size * 0.3} width={size * 2} height={size * 0.45}
          fill={color} stroke={MAP_COLORS.ink} strokeWidth={0.8} vectorEffect="non-scaling-stroke" />
  );
}
