"use client";

import { Crosshair } from "lucide-react";
import type { Trails } from "@/hooks/useUnitTrails";
import { DAMAGED_COLOR, DESTROYED_COLOR, SIDE_COLORS, cn } from "@/lib/presentation";
import type { Battlefield } from "@/lib/types";
import { BattlefieldMap, type ViewMode } from "./BattlefieldMap";
import { Panel } from "./ui";

const VIEW_MODES: { value: ViewMode; label: string; hint: string }[] = [
  { value: "ARBITER", label: "Арбітр", hint: "Повна картина: усі юніти та всі зони огляду" },
  { value: "BLUE", label: "Очима Blue", hint: "Лише те, що брокер повертає симулятору Blue Force" },
  { value: "RED", label: "Очима Red", hint: "Лише те, що брокер повертає симулятору Red Force" },
];

function ViewModeSwitch({ value, onChange }: { value: ViewMode; onChange: (mode: ViewMode) => void }) {
  return (
    <div role="radiogroup" aria-label="Режим перегляду" className="flex rounded-lg border border-slate-700 bg-slate-950 p-0.5">
      {VIEW_MODES.map((mode) => (
        <button key={mode.value} type="button" role="radio" aria-checked={value === mode.value} title={mode.hint}
                onClick={() => onChange(mode.value)}
                className={cn(
                  "whitespace-nowrap rounded-md px-2.5 py-1 text-xs font-medium transition-colors",
                  value === mode.value ? "bg-slate-700 text-white" : "text-slate-400 hover:text-slate-200",
                )}>
          {mode.label}
        </button>
      ))}
    </div>
  );
}

function LegendItem({ children, label }: { children: React.ReactNode; label: string }) {
  return (
    <li className="flex items-center gap-1.5">
      <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden>{children}</svg>
      {label}
    </li>
  );
}

function Legend() {
  return (
    <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-slate-400">
      <LegendItem label="Blue Force">
        <rect x="2" y="4" width="12" height="8" rx="1" fill={SIDE_COLORS.BLUE} fillOpacity="0.3" stroke={SIDE_COLORS.BLUE} strokeWidth="1.5" />
      </LegendItem>
      <LegendItem label="Red Force">
        <polygon points="8,2 14,8 8,14 2,8" fill={SIDE_COLORS.RED} fillOpacity="0.3" stroke={SIDE_COLORS.RED} strokeWidth="1.5" />
      </LegendItem>
      <LegendItem label="Зона огляду">
        <circle cx="8" cy="8" r="6" fill="#94a3b8" fillOpacity="0.1" stroke="#94a3b8" strokeDasharray="2 2" />
      </LegendItem>
      <LegendItem label="Виявлений противником">
        <circle cx="8" cy="8" r="6" fill="none" stroke="#e2e8f0" strokeDasharray="2 2" />
      </LegendItem>
      <LegendItem label="Пошкоджений (радіус ×0.5)">
        <rect x="2" y="4" width="12" height="8" rx="1" fill="none" stroke={DAMAGED_COLOR} strokeWidth="1.5" />
      </LegendItem>
      <LegendItem label="Знищений">
        <rect x="2" y="4" width="12" height="8" rx="1" fill={DESTROYED_COLOR} fillOpacity="0.3" stroke={DESTROYED_COLOR} strokeWidth="1.5" />
        <path d="M4 4 L12 12 M12 4 L4 12" stroke="#e2e8f0" strokeWidth="1.2" />
      </LegendItem>
    </ul>
  );
}

export function BattlefieldPanel({ battlefield, trails, viewMode, onViewModeChange }: {
  battlefield: Battlefield | null;
  trails: Trails;
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
}) {
  const hint = VIEW_MODES.find((mode) => mode.value === viewMode)?.hint;

  return (
    <Panel title="Поле бою" icon={<Crosshair className="h-4 w-4" aria-hidden />}
           actions={<ViewModeSwitch value={viewMode} onChange={onViewModeChange} />}>
      <p className="mb-3 text-xs text-slate-500">{hint}</p>
      <BattlefieldMap battlefield={battlefield} trails={trails} viewMode={viewMode} />
      <Legend />
    </Panel>
  );
}
