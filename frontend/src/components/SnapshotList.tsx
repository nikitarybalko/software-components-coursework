"use client";

import { Archive, History } from "lucide-react";
import { SIDE_COLORS, formatTime } from "@/lib/presentation";
import type { SnapshotInfo } from "@/lib/types";
import type { RunAction } from "./ControlPanel";
import { ConfirmButton, Panel } from "./ui";

export function SnapshotList({ snapshots, busyAction, onAction, restore }: {
  snapshots: SnapshotInfo[];
  busyAction: string | null;
  onAction: RunAction;
  restore: (snapshotId: string) => Promise<unknown>;
}) {
  return (
    <Panel title="Snapshot'и" icon={<Archive className="h-4 w-4" aria-hidden />}
           actions={<span className="text-xs text-slate-500">{snapshots.length}</span>}>
      {snapshots.length === 0 ? (
        <p className="text-sm text-slate-500">Snapshot'и ще не збережено. Брокер зберігає їх автоматично кожні N тіків.</p>
      ) : (
        <ul className="-mr-2 max-h-72 space-y-2 overflow-y-auto pr-2">
          {snapshots.map((snapshot) => {
            const blue = snapshot.units.filter((unit) => unit.side === "BLUE").length;
            const red = snapshot.units.length - blue;
            const actionKey = `restore:${snapshot.id}`;
            return (
              <li key={snapshot.id} className="flex items-center justify-between gap-3 rounded-lg border border-slate-800 bg-slate-950/50 px-3 py-2">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-slate-100">
                    Тік <span className="font-mono tabular-nums">{snapshot.tickNumber}</span>
                  </p>
                  <p className="text-xs text-slate-500">
                    {formatTime(snapshot.createdAt)} ·{" "}
                    <span style={{ color: SIDE_COLORS.BLUE }}>{blue}</span> /{" "}
                    <span style={{ color: SIDE_COLORS.RED }}>{red}</span> юнітів
                  </p>
                </div>
                <ConfirmButton className="shrink-0 px-2.5 py-1.5 text-xs" icon={<History className="h-3.5 w-3.5" aria-hidden />}
                               busy={busyAction === actionKey} confirmLabel={`Тік ${snapshot.tickNumber}?`}
                               onConfirm={() => onAction(actionKey, () => restore(snapshot.id))}>
                  Відновити
                </ConfirmButton>
              </li>
            );
          })}
        </ul>
      )}
      <p className="mt-3 text-xs text-slate-500">Після відновлення симуляція стоїть на паузі. Натисніть «Продовжити».</p>
    </Panel>
  );
}
