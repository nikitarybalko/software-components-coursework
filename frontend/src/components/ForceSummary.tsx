import { Shield } from "lucide-react";
import { DAMAGED_COLOR, DESTROYED_COLOR, SIDE_COLORS, SIDE_LABELS } from "@/lib/presentation";
import type { Battlefield, Side, Unit } from "@/lib/types";
import { Panel } from "./ui";

function countByStatus(units: Unit[]) {
  return {
    healthy: units.filter((unit) => unit.status === "HEALTHY").length,
    damaged: units.filter((unit) => unit.status === "DAMAGED").length,
    destroyed: units.filter((unit) => unit.status === "DESTROYED").length,
  };
}

function ForceRow({ side, units, contacts }: { side: Side; units: Unit[]; contacts: number }) {
  const counts = countByStatus(units);
  const total = Math.max(units.length, 1);
  const segments = [
    { value: counts.healthy, color: SIDE_COLORS[side], label: "боєздатні" },
    { value: counts.damaged, color: DAMAGED_COLOR, label: "пошкоджені" },
    { value: counts.destroyed, color: DESTROYED_COLOR, label: "знищені" },
  ];

  return (
    <div>
      <div className="flex items-baseline justify-between">
        <p className="text-sm font-semibold" style={{ color: SIDE_COLORS[side] }}>{SIDE_LABELS[side]}</p>
        <p className="text-xs text-slate-400">
          бачить противника: <span className="font-mono tabular-nums text-slate-200">{contacts}</span>
        </p>
      </div>
      <div className="mt-2 flex h-2 overflow-hidden rounded-full bg-slate-800" aria-hidden>
        {segments.map((segment) => (
          <div key={segment.label} style={{ width: `${(segment.value / total) * 100}%`, backgroundColor: segment.color }} />
        ))}
      </div>
      <dl className="mt-2 grid grid-cols-3 gap-2 text-xs">
        {segments.map((segment) => (
          <div key={segment.label}>
            <dt className="text-slate-500">{segment.label}</dt>
            <dd className="font-mono text-base tabular-nums text-slate-100">{segment.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

export function ForceSummary({ battlefield }: { battlefield: Battlefield | null }) {
  return (
    <Panel title="Сили сторін" icon={<Shield className="h-4 w-4" aria-hidden />}>
      <div className="space-y-5">
        <ForceRow side="BLUE" units={battlefield?.blueUnits ?? []} contacts={battlefield?.blueVisibleRed.length ?? 0} />
        <ForceRow side="RED" units={battlefield?.redUnits ?? []} contacts={battlefield?.redVisibleBlue.length ?? 0} />
      </div>
    </Panel>
  );
}
