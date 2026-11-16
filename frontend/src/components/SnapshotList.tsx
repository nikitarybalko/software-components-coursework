"use client";

import { formatTick, formatTime } from "@/lib/presentation";
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
    <Panel index={4} title="Контрольні точки" bodyClassName="p-0"
           aside={<span className="font-mono text-xs text-ink-faint">{snapshots.length}</span>}>
      {snapshots.length === 0 ? (
        <p className="px-3 py-3 text-sm text-ink-soft">Ще немає. Брокер зберігає точку автоматично кожні кілька тіків.</p>
      ) : (
        <div className="max-h-72 overflow-y-auto">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-sheet">
              <tr className="border-b border-rule font-heading text-[11px] uppercase text-ink-faint">
                <th scope="col" className="px-3 py-1.5 text-left font-medium">Тік</th>
                <th scope="col" className="px-2 py-1.5 text-left font-medium">Час</th>
                <th scope="col" className="px-2 py-1.5 text-right font-medium">B / R</th>
                <th scope="col" className="px-3 py-1.5"><span className="sr-only">Дія</span></th>
              </tr>
            </thead>
            <tbody className="font-mono tabular-nums">
              {snapshots.map((snapshot) => {
                const blue = snapshot.units.filter((unit) => unit.side === "BLUE").length;
                const actionKey = `restore:${snapshot.id}`;
                return (
                  <tr key={snapshot.id} className="border-b border-rule-soft last:border-b-0">
                    <td className="px-3 py-1">{formatTick(snapshot.tickNumber)}</td>
                    <td className="px-2 py-1 text-ink-soft">{formatTime(snapshot.createdAt)}</td>
                    <td className="px-2 py-1 text-right text-ink-soft">{blue} / {snapshot.units.length - blue}</td>
                    <td className="px-3 py-1 text-right">
                      <ConfirmButton className="px-2 py-0.5 text-xs" busy={busyAction === actionKey}
                                     confirmLabel={`Тік ${snapshot.tickNumber}?`}
                                     onConfirm={() => onAction(actionKey, () => restore(snapshot.id))}>
                        Відновити
                      </ConfirmButton>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      <p className="border-t border-rule px-3 py-1.5 text-xs text-ink-soft">
        Після відновлення сесія стоїть на паузі до команди «Продовжити».
      </p>
    </Panel>
  );
}
