import { MAP_COLORS } from "@/lib/presentation";
import { GRID_STEP, viewportHeight, type Bounds, type Viewport } from "./geometry";

/** MGRS-style column letters skip I and O so they are not confused with 1 and 0. */
const COLUMN_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
const SCALE_BAR_LENGTHS = [1, 2, 5, 10, 20, 25, 50, 100, 200];
const SCALE_BAR_MAX_SHEET_LENGTH = 60;

export const COLLAR = 9;
export const FOOTER = 12;

interface GridLabel {
  key: number;
  text: string;
  position: number;
}

/** Labels for every grid square that is at least partly visible, centred on its visible part. */
function gridLabels(sheetStart: number, sheetLength: number, viewStart: number, viewLength: number, zoom: number,
                    minVisible: number, toText: (index: number) => string): GridLabel[] {
  const firstSheetSquare = Math.floor(sheetStart / GRID_STEP);
  const lastSheetSquare = Math.ceil((sheetStart + sheetLength) / GRID_STEP) - 1;
  const first = Math.max(Math.floor(viewStart / GRID_STEP), firstSheetSquare);
  const last = Math.min(Math.ceil((viewStart + viewLength) / GRID_STEP) - 1, lastSheetSquare);
  return Array.from({ length: Math.max(last - first + 1, 0) }, (_, i) => first + i)
    .map((square) => {
      const visibleStart = Math.max(square * GRID_STEP, viewStart);
      const visibleEnd = Math.min((square + 1) * GRID_STEP, viewStart + viewLength);
      return {
        key: square,
        text: toText(square - firstSheetSquare),
        position: sheetStart + ((visibleStart + visibleEnd) / 2 - viewStart) * zoom,
        visibleLength: (visibleEnd - visibleStart) * zoom,
      };
    })
    .filter((label) => label.visibleLength >= minVisible);
}

export function MapCollar({ bounds, viewport, zoom, sheetScale }: {
  bounds: Bounds;
  viewport: Viewport;
  zoom: number;
  sheetScale: number;
}) {
  const fontSize = 3.4 * sheetScale;
  const half = (COLLAR * sheetScale) / 2;
  const maxX = bounds.minX + bounds.width;
  const maxY = bounds.minY + bounds.height;
  const columns = gridLabels(bounds.minX, bounds.width, viewport.x, viewport.width, zoom, fontSize * 1.5,
    (index) => COLUMN_LETTERS[index] ?? "");
  const rows = gridLabels(bounds.minY, bounds.height, viewport.y, viewportHeight(viewport, bounds), zoom, fontSize * 1.5,
    (index) => String(index + 1));
  const label = { fontSize, fill: MAP_COLORS.inkSoft, textAnchor: "middle" as const, dominantBaseline: "central" as const };

  return (
    <g>
      {columns.map((column) => (
        <g key={`c${column.key}`}>
          <text x={column.position} y={bounds.minY - half} {...label}>{column.text}</text>
          <text x={column.position} y={maxY + half} {...label}>{column.text}</text>
        </g>
      ))}
      {rows.map((row) => (
        <g key={`r${row.key}`}>
          <text x={bounds.minX - half} y={row.position} {...label}>{row.text}</text>
          <text x={maxX + half} y={row.position} {...label}>{row.text}</text>
        </g>
      ))}
    </g>
  );
}

function scaleBarLength(zoom: number, sheetScale: number) {
  const fitting = SCALE_BAR_LENGTHS.filter((length) => length * zoom <= SCALE_BAR_MAX_SHEET_LENGTH * sheetScale);
  return fitting.at(-1) ?? SCALE_BAR_LENGTHS[0];
}

/** Scale bar and north arrow in the bottom margin, outside the sheet, so map layers never cover them. */
export function MapFooter({ bounds, zoom, sheetScale }: { bounds: Bounds; zoom: number; sheetScale: number }) {
  const worldLength = scaleBarLength(zoom, sheetScale);
  const length = worldLength * zoom;
  const x = bounds.minX;
  const baseline = bounds.minY + bounds.height + COLLAR * sheetScale + (FOOTER * sheetScale) / 2;
  const barHeight = 1.6 * sheetScale;
  const fontSize = 3 * sheetScale;
  const segment = { height: barHeight, y: baseline, stroke: MAP_COLORS.ink, vectorEffect: "non-scaling-stroke" as const };
  const arrowX = bounds.minX + bounds.width - 4 * sheetScale;
  const arrow = 3 * sheetScale;
  const arrowTop = baseline - arrow * 1.2;

  return (
    <g>
      <rect x={x} width={length / 2} fill={MAP_COLORS.ink} {...segment} />
      <rect x={x + length / 2} width={length / 2} fill={MAP_COLORS.sheet} {...segment} />
      {[0, worldLength / 2, worldLength].filter(Number.isInteger).map((value) => (
        <text key={value} x={x + value * zoom} y={baseline - 1.5 * sheetScale} fontSize={fontSize} textAnchor="middle"
              fill={MAP_COLORS.ink}>
          {value}
        </text>
      ))}
      <text x={x + length + 2.5 * sheetScale} y={baseline + barHeight} fontSize={fontSize} fill={MAP_COLORS.ink}>од.</text>

      <polygon points={`${arrowX},${arrowTop} ${arrowX + arrow * 0.6},${arrowTop + arrow * 1.8} ${arrowX},${arrowTop + arrow * 1.4}`}
               fill={MAP_COLORS.ink} />
      <polygon points={`${arrowX},${arrowTop} ${arrowX - arrow * 0.6},${arrowTop + arrow * 1.8} ${arrowX},${arrowTop + arrow * 1.4}`}
               fill={MAP_COLORS.sheet} stroke={MAP_COLORS.ink} strokeWidth={0.8} vectorEffect="non-scaling-stroke" />
      <text x={arrowX - arrow * 1.2} y={arrowTop + arrow} fontSize={fontSize} textAnchor="end" dominantBaseline="central"
            fill={MAP_COLORS.ink}>Пн</text>
    </g>
  );
}
