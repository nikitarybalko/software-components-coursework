import { useEffect, useRef, useState } from "react";
import { unitKey } from "@/lib/presentation";
import type { Battlefield, Point, Unit } from "@/lib/types";

export type Trails = Record<string, Point[]>;

const MAX_TRAIL_LENGTH = 25;

function appendPosition(points: Point[] | undefined, unit: Unit): Point[] {
  const history = points ?? [];
  const last = history.at(-1);
  if (last && last.x === unit.x && last.y === unit.y) {
    return history;
  }
  return [...history, { x: unit.x, y: unit.y }].slice(-MAX_TRAIL_LENGTH);
}

/** Accumulates unit positions per completed tick; starts over whenever the broker state epoch changes. */
export function useUnitTrails(battlefield: Battlefield | null, epoch: number | undefined): Trails {
  const [trails, setTrails] = useState<Trails>({});
  const lastTickRef = useRef<number | null>(null);
  const lastEpochRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (!battlefield || (lastTickRef.current === battlefield.currentTick && lastEpochRef.current === epoch)) {
      return;
    }
    const stateReplaced = lastEpochRef.current !== undefined && lastEpochRef.current !== epoch;
    lastTickRef.current = battlefield.currentTick;
    lastEpochRef.current = epoch;
    const units = [...battlefield.blueUnits, ...battlefield.redUnits];

    setTrails((current) => {
      const base = stateReplaced ? {} : current;
      return Object.fromEntries(units.map((unit) => [unitKey(unit), appendPosition(base[unitKey(unit)], unit)]));
    });
  }, [battlefield, epoch]);

  return trails;
}
