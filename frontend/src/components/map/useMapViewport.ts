import { useCallback, useEffect, useState } from "react";
import type { Point, Unit } from "@/lib/types";
import {
  clampViewport,
  fitViewport,
  fullViewport,
  viewportCenter,
  zoomOf,
  zoomViewport,
  type Bounds,
  type Viewport,
} from "./geometry";

export const BUTTON_ZOOM_STEP = 1.5;
export const WHEEL_ZOOM_STEP = 1.15;

export function useMapViewport(bounds: Bounds) {
  const [viewport, setViewport] = useState<Viewport>(() => fullViewport(bounds));

  useEffect(() => {
    setViewport((current) => clampViewport(current, bounds));
  }, [bounds]);

  const zoomAt = useCallback((factor: number, anchor: Point) => {
    setViewport((current) => zoomViewport(current, bounds, factor, anchor));
  }, [bounds]);

  const zoomAtCenter = useCallback((factor: number) => {
    setViewport((current) => zoomViewport(current, bounds, factor, viewportCenter(current, bounds)));
  }, [bounds]);

  const panBy = useCallback((dx: number, dy: number) => {
    setViewport((current) => clampViewport({ ...current, x: current.x + dx, y: current.y + dy }, bounds));
  }, [bounds]);

  const showAll = useCallback(() => setViewport(fullViewport(bounds)), [bounds]);

  const fitUnits = useCallback((units: Unit[]) => setViewport(fitViewport(units, bounds)), [bounds]);

  return {
    viewport,
    zoom: zoomOf(viewport, bounds),
    zoomAt,
    zoomIn: () => zoomAtCenter(BUTTON_ZOOM_STEP),
    zoomOut: () => zoomAtCenter(1 / BUTTON_ZOOM_STEP),
    panBy,
    showAll,
    fitUnits,
  };
}
