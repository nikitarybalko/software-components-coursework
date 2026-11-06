"use client";

import { useCallback, useEffect, useId, useMemo, useRef, type PointerEvent } from "react";
import type { Trails } from "@/hooks/useUnitTrails";
import { MAP_COLORS, cn } from "@/lib/presentation";
import type { Battlefield, Point } from "@/lib/types";
import { WORLD_WIDTH, sheetBounds, type Bounds, type Viewport } from "./geometry";
import { COLLAR, FOOTER, MapCollar, MapFooter } from "./MapFrame";
import { GridLines, SceneLayers, buildScene, type ViewMode } from "./MapLayers";
import { WHEEL_ZOOM_STEP, useMapViewport } from "./useMapViewport";

export type { ViewMode } from "./MapLayers";

const BASE_MARKER_SIZE = 3.4;
const ZOOM_EPSILON = 1.001;

interface DragState {
  pointerId: number;
  lastX: number;
  lastY: number;
}

function ZoomControls({ zoom, onZoomIn, onZoomOut, onFitUnits, onShowAll, canFit }: {
  zoom: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitUnits: () => void;
  onShowAll: () => void;
  canFit: boolean;
}) {
  const button = "px-2 py-0.5 font-heading text-xs font-medium hover:bg-paper disabled:cursor-not-allowed disabled:opacity-35";
  return (
    <div className="flex items-center gap-2">
      <span className="font-mono text-xs tabular-nums text-ink-soft" aria-live="polite">×{zoom.toFixed(1)}</span>
      <div className="flex border border-ink bg-sheet" role="group" aria-label="Масштаб карти">
        <button type="button" className={cn(button, "w-6")} onClick={onZoomOut} disabled={zoom <= ZOOM_EPSILON}
                aria-label="Зменшити">−</button>
        <button type="button" className={cn(button, "w-6 border-l border-ink")} onClick={onZoomIn} aria-label="Збільшити">+</button>
        <button type="button" className={cn(button, "border-l border-ink")} onClick={onFitUnits} disabled={!canFit}>Усі юніти</button>
        <button type="button" className={cn(button, "border-l border-ink")} onClick={onShowAll} disabled={zoom <= ZOOM_EPSILON}>
          Уся карта
        </button>
      </div>
    </div>
  );
}

function worldTransform(bounds: Bounds, viewport: Viewport, zoom: number) {
  return `translate(${bounds.minX} ${bounds.minY}) scale(${zoom}) translate(${-viewport.x} ${-viewport.y})`;
}

interface BattlefieldMapProps {
  battlefield: Battlefield | null;
  trails: Trails;
  viewMode: ViewMode;
  hint?: string;
}

export function BattlefieldMap({ battlefield, trails, viewMode, hint }: BattlefieldMapProps) {
  const id = `map-${useId().replace(/:/g, "")}`;
  const svgRef = useRef<SVGSVGElement>(null);
  const dragRef = useRef<DragState | null>(null);

  const allUnits = useMemo(() => (battlefield ? [...battlefield.blueUnits, ...battlefield.redUnits] : []), [battlefield]);
  const boundsKey = JSON.stringify(sheetBounds(allUnits));
  const bounds = useMemo<Bounds>(() => JSON.parse(boundsKey), [boundsKey]);
  const scene = useMemo(() => (battlefield ? buildScene(battlefield, viewMode) : null), [battlefield, viewMode]);
  const { viewport, zoom, zoomAt, zoomIn, zoomOut, panBy, showAll, fitUnits } = useMapViewport(bounds);

  const sheetScale = bounds.width / WORLD_WIDTH;
  const markerSize = (BASE_MARKER_SIZE * sheetScale) / Math.sqrt(zoom);
  const collar = COLLAR * sheetScale;
  const viewBox = {
    x: bounds.minX - collar,
    y: bounds.minY - collar,
    width: bounds.width + collar * 2,
    height: bounds.height + collar * 2 + FOOTER * sheetScale,
  };

  const latest = useRef({ viewport, zoom, viewBox, bounds });
  latest.current = { viewport, zoom, viewBox, bounds };

  /** Converts a screen point into world coordinates of the current view. */
  const toWorld = useCallback((clientX: number, clientY: number): Point => {
    const { viewport: view, zoom: currentZoom, viewBox: box, bounds: sheet } = latest.current;
    const rect = svgRef.current!.getBoundingClientRect();
    const sheetX = box.x + ((clientX - rect.left) * box.width) / rect.width;
    const sheetY = box.y + ((clientY - rect.top) * box.height) / rect.height;
    return { x: view.x + (sheetX - sheet.minX) / currentZoom, y: view.y + (sheetY - sheet.minY) / currentZoom };
  }, []);

  useEffect(() => {
    const svg = svgRef.current;
    if (!svg) {
      return;
    }
    const onWheel = (event: WheelEvent) => {
      if (!event.ctrlKey && !event.metaKey) {
        return;
      }
      event.preventDefault();
      zoomAt(event.deltaY < 0 ? WHEEL_ZOOM_STEP : 1 / WHEEL_ZOOM_STEP, toWorld(event.clientX, event.clientY));
    };
    svg.addEventListener("wheel", onWheel, { passive: false });
    return () => svg.removeEventListener("wheel", onWheel);
  }, [zoomAt, toWorld]);

  const zoomed = zoom > ZOOM_EPSILON;

  const handlePointerDown = (event: PointerEvent<SVGSVGElement>) => {
    if (!zoomed) {
      return;
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    dragRef.current = { pointerId: event.pointerId, lastX: event.clientX, lastY: event.clientY };
  };

  const handlePointerMove = (event: PointerEvent<SVGSVGElement>) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }
    const start = toWorld(drag.lastX, drag.lastY);
    const end = toWorld(event.clientX, event.clientY);
    dragRef.current = { ...drag, lastX: event.clientX, lastY: event.clientY };
    panBy(start.x - end.x, start.y - end.y);
  };

  const endDrag = () => {
    dragRef.current = null;
  };

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 border-b border-rule-soft px-3 py-1">
        <p className="text-xs text-ink-soft">{hint}</p>
        <ZoomControls zoom={zoom} onZoomIn={zoomIn} onZoomOut={zoomOut} onShowAll={showAll}
                      onFitUnits={() => fitUnits(allUnits)} canFit={allUnits.length > 0} />
      </div>
      <div className="relative bg-paper">
        <svg ref={svgRef} viewBox={`${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`}
             className={cn("block h-auto w-full select-none", zoomed && "cursor-grab active:cursor-grabbing")}
             style={{ fontFamily: "var(--font-plex-mono)", touchAction: zoomed ? "none" : "auto" }}
             role="img" aria-label="Карта обстановки"
             onPointerDown={handlePointerDown} onPointerMove={handlePointerMove}
             onPointerUp={endDrag} onPointerCancel={endDrag}>
          <defs>
            <clipPath id={`${id}-sheet`}>
              <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} />
            </clipPath>
          </defs>
          <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} fill={MAP_COLORS.sheet} />
          <g clipPath={`url(#${id}-sheet)`}>
            <g transform={worldTransform(bounds, viewport, zoom)}>
              <GridLines bounds={bounds} />
              {scene && (
                <SceneLayers scene={scene} trails={trails} bounds={bounds} zoom={zoom} markerSize={markerSize} fogId={id} />
              )}
            </g>
          </g>
          <rect x={bounds.minX} y={bounds.minY} width={bounds.width} height={bounds.height} fill="none"
                stroke={MAP_COLORS.ink} strokeWidth={1.5} vectorEffect="non-scaling-stroke" />
          <MapCollar bounds={bounds} viewport={viewport} zoom={zoom} sheetScale={sheetScale} />
          <MapFooter bounds={bounds} zoom={zoom} sheetScale={sheetScale} />
        </svg>
        {allUnits.length === 0 && (
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center px-4">
            <div className="border border-ink bg-sheet px-4 py-2 text-center">
              <p className="font-heading text-sm font-semibold uppercase">Обстановка відсутня</p>
              <p className="text-xs text-ink-soft">Ініціалізуйте сесію та запустіть симулятори Blue і Red Force</p>
            </div>
          </div>
        )}
      </div>
      <p className="border-t border-rule-soft px-3 py-1 text-[11px] text-ink-faint">
        Ctrl + коліщатко або жест на тачпаді змінює масштаб, перетягування зсуває збільшену карту.
      </p>
    </div>
  );
}
