import type { Point, Unit } from "@/lib/types";

/** Size of the simulators' ContinuousSpace; the sheet only grows if a unit is reported outside it. */
export const WORLD_WIDTH = 300;
export const WORLD_HEIGHT = 200;
export const GRID_STEP = 25;
export const MAX_ZOOM = 8;
export const FIT_PADDING = 20;
/** Share of the visible width a zoomed-in view may be panned past the sheet edge, so edge units are not glued to the frame. */
export const PAN_MARGIN_RATIO = 0.15;
/** Extent of the off-sheet backdrop; larger than any pan margin the ratio can produce. */
export const OFF_SHEET_EXTENT = 25;

export interface Bounds {
  minX: number;
  minY: number;
  width: number;
  height: number;
}

/** Visible window of the world; its height follows the sheet's aspect ratio. */
export interface Viewport {
  x: number;
  y: number;
  width: number;
}

function snapDown(value: number) {
  return Math.floor(value / GRID_STEP) * GRID_STEP;
}

function snapUp(value: number) {
  return Math.ceil(value / GRID_STEP) * GRID_STEP;
}

export function sheetBounds(units: Unit[]): Bounds {
  const xs = units.map((unit) => unit.x);
  const ys = units.map((unit) => unit.y);
  const minX = Math.min(0, ...xs.map(snapDown));
  const minY = Math.min(0, ...ys.map(snapDown));
  const maxX = Math.max(WORLD_WIDTH, ...xs.map(snapUp));
  const maxY = Math.max(WORLD_HEIGHT, ...ys.map(snapUp));
  return { minX, minY, width: maxX - minX, height: maxY - minY };
}

export function viewportHeight(viewport: Viewport, bounds: Bounds) {
  return (viewport.width * bounds.height) / bounds.width;
}

export function zoomOf(viewport: Viewport, bounds: Bounds) {
  return bounds.width / viewport.width;
}

export function fullViewport(bounds: Bounds): Viewport {
  return { x: bounds.minX, y: bounds.minY, width: bounds.width };
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

/** Pan margin follows the visible width and fades out towards the whole-sheet view. */
function panMargin(width: number, bounds: Bounds) {
  return PAN_MARGIN_RATIO * width * (1 - width / bounds.width);
}

export function clampViewport(viewport: Viewport, bounds: Bounds): Viewport {
  const width = clamp(viewport.width, bounds.width / MAX_ZOOM, bounds.width);
  const height = viewportHeight({ ...viewport, width }, bounds);
  const margin = panMargin(width, bounds);
  return {
    width,
    x: clamp(viewport.x, bounds.minX - margin, bounds.minX + bounds.width - width + margin),
    y: clamp(viewport.y, bounds.minY - margin, bounds.minY + bounds.height - height + margin),
  };
}

/** Zooms by the factor while keeping the anchor point at the same place on screen. */
export function zoomViewport(viewport: Viewport, bounds: Bounds, factor: number, anchor: Point): Viewport {
  const width = viewport.width / factor;
  const ratio = width / viewport.width;
  return clampViewport({
    width,
    x: anchor.x - (anchor.x - viewport.x) * ratio,
    y: anchor.y - (anchor.y - viewport.y) * ratio,
  }, bounds);
}

export function viewportCenter(viewport: Viewport, bounds: Bounds): Point {
  return { x: viewport.x + viewport.width / 2, y: viewport.y + viewportHeight(viewport, bounds) / 2 };
}

export function fitViewport(units: Unit[], bounds: Bounds): Viewport {
  if (units.length === 0) {
    return fullViewport(bounds);
  }
  const xs = units.map((unit) => unit.x);
  const ys = units.map((unit) => unit.y);
  const minX = Math.min(...xs) - FIT_PADDING;
  const minY = Math.min(...ys) - FIT_PADDING;
  const spanX = Math.max(...xs) + FIT_PADDING - minX;
  const spanY = Math.max(...ys) + FIT_PADDING - minY;
  const width = Math.max(spanX, (spanY * bounds.width) / bounds.height);
  const height = (width * bounds.height) / bounds.width;
  return clampViewport({
    width,
    x: minX + spanX / 2 - width / 2,
    y: minY + spanY / 2 - height / 2,
  }, bounds);
}
