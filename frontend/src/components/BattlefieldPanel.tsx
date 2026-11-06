"use client";

import type { ReactNode } from "react";
import type { Trails } from "@/hooks/useUnitTrails";
import { MAP_COLORS, SIDE_COLORS, SIDE_FILLS, UNIT_TYPE_LABELS, cn } from "@/lib/presentation";
import type { Battlefield, UnitType } from "@/lib/types";
import { BattlefieldMap, type ViewMode } from "./map/BattlefieldMap";
import { UnitPictogram } from "./map/UnitSymbols";
import { Panel } from "./ui";

const VIEW_MODES: { value: ViewMode; label: string; hint: string }[] = [
  { value: "ARBITER", label: "Арбітр", hint: "Повна обстановка: усі юніти та всі зони огляду." },
  { value: "BLUE", label: "Очима Blue", hint: "Лише те, що брокер повертає симулятору Blue Force. Заштриховане поза зоною огляду." },
  { value: "RED", label: "Очима Red", hint: "Лише те, що брокер повертає симулятору Red Force. Заштриховане поза зоною огляду." },
];

const LEGEND_TYPES: UnitType[] = ["ARMOR", "MECHANIZED_INFANTRY", "INFANTRY", "RECON", "UNKNOWN"];
const INK = MAP_COLORS.ink;

function ViewModeSwitch({ value, onChange }: { value: ViewMode; onChange: (mode: ViewMode) => void }) {
  return (
    <div role="radiogroup" aria-label="Режим перегляду" className="flex border border-ink">
      {VIEW_MODES.map((mode, index) => (
        <button key={mode.value} type="button" role="radio" aria-checked={value === mode.value}
                onClick={() => onChange(mode.value)}
                className={cn(
                  "whitespace-nowrap px-2.5 py-0.5 font-heading text-xs font-medium",
                  index > 0 && "border-l border-ink",
                  value === mode.value ? "bg-ink text-sheet" : "bg-sheet text-ink hover:bg-paper",
                )}>
          {mode.label}
        </button>
      ))}
    </div>
  );
}

function LegendItem({ children, label }: { children: ReactNode; label: string }) {
  return (
    <li className="flex items-center gap-1.5">
      <svg viewBox="0 0 22 16" className="h-4 w-[22px] shrink-0" aria-hidden>{children}</svg>
      {label}
    </li>
  );
}

function FriendFrame({ fill = SIDE_FILLS.BLUE, height = 13 }: { fill?: string; height?: number }) {
  return <rect x="1" y="1.5" width="20" height={height} fill={fill} stroke={INK} strokeWidth="1" />;
}

function LegendGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
      <span className="w-16 shrink-0 font-heading text-[11px] font-medium uppercase text-ink-faint">{title}</span>
      <ul className="flex flex-wrap gap-x-4 gap-y-1">{children}</ul>
    </div>
  );
}

function Legend() {
  return (
    <div className="space-y-1.5 border-t border-ink px-3 py-2 text-xs text-ink-soft">
      <LegendGroup title="Сторона">
        <LegendItem label="Blue Force"><FriendFrame /></LegendItem>
        <LegendItem label="Red Force">
          <polygon points="11,1 18,8 11,15 4,8" fill={SIDE_FILLS.RED} stroke={INK} strokeWidth="1" />
        </LegendItem>
        <LegendItem label="Зона огляду">
          <circle cx="11" cy="8" r="6.5" fill={SIDE_FILLS.BLUE} fillOpacity="0.2" stroke={SIDE_COLORS.BLUE} strokeDasharray="2 1.5" />
        </LegendItem>
        <LegendItem label="Виявлений противником">
          <circle cx="11" cy="8" r="6.5" fill="none" stroke={SIDE_COLORS.RED} strokeDasharray="1.5 1.5" />
        </LegendItem>
      </LegendGroup>
      <LegendGroup title="Тип">
        {LEGEND_TYPES.map((type) => (
          <LegendItem key={type} label={UNIT_TYPE_LABELS[type]}>
            <FriendFrame />
            <UnitPictogram type={type} box={{ cx: 11, cy: 8, halfWidth: 9.5, halfHeight: 6.3 }} color={INK} hole={SIDE_FILLS.BLUE} />
          </LegendItem>
        ))}
      </LegendGroup>
      <LegendGroup title="Стан">
        <LegendItem label="пошкоджений, огляд ×0.5">
          <FriendFrame height={9} />
          <rect x="1" y="12" width="20" height="3" fill={MAP_COLORS.conditionDamaged} stroke={INK} strokeWidth="0.6" />
        </LegendItem>
        <LegendItem label="знищений">
          <FriendFrame fill={MAP_COLORS.destroyedFill} height={9} />
          <rect x="1" y="12" width="20" height="3" fill={MAP_COLORS.conditionDestroyed} stroke={INK} strokeWidth="0.6" />
        </LegendItem>
      </LegendGroup>
    </div>
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
    <Panel index={1} title="Обстановка" bodyClassName="p-0"
           aside={<ViewModeSwitch value={viewMode} onChange={onViewModeChange} />}>
      <BattlefieldMap battlefield={battlefield} trails={trails} viewMode={viewMode} hint={hint} />
      <Legend />
    </Panel>
  );
}
