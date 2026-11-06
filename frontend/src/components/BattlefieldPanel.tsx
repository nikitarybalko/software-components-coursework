"use client";

import type { ReactNode } from "react";
import type { Trails } from "@/hooks/useUnitTrails";
import { MAP_COLORS, SIDE_COLORS, SIDE_FILLS, cn } from "@/lib/presentation";
import type { Battlefield } from "@/lib/types";
import { BattlefieldMap, type ViewMode } from "./BattlefieldMap";
import { Panel } from "./ui";

const VIEW_MODES: { value: ViewMode; label: string; hint: string }[] = [
  { value: "ARBITER", label: "Арбітр", hint: "Повна обстановка: усі юніти та всі зони огляду." },
  { value: "BLUE", label: "Очима Blue", hint: "Лише те, що брокер повертає симулятору Blue Force. Заштриховане поза зоною огляду." },
  { value: "RED", label: "Очима Red", hint: "Лише те, що брокер повертає симулятору Red Force. Заштриховане поза зоною огляду." },
];

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

const INK = MAP_COLORS.ink;

function LegendItem({ children, label }: { children: ReactNode; label: string }) {
  return (
    <li className="flex items-center gap-1.5">
      <svg viewBox="0 0 18 14" className="h-3.5 w-[18px] shrink-0" aria-hidden>{children}</svg>
      {label}
    </li>
  );
}

function FriendFrame({ fill = SIDE_FILLS.BLUE, children }: { fill?: string; children?: ReactNode }) {
  return (
    <>
      <rect x="2" y="2.5" width="14" height="9" fill={fill} stroke={INK} strokeWidth="1" />
      {children}
    </>
  );
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
  const cross = <path d="M2 2.5 L16 11.5 M16 2.5 L2 11.5" stroke={INK} strokeWidth="1" />;
  const track = <ellipse cx="9" cy="7" rx="4.2" ry="2" fill="none" stroke={INK} strokeWidth="1" />;

  return (
    <div className="space-y-1.5 border-t border-ink px-3 py-2 text-xs text-ink-soft">
      <LegendGroup title="Сторона">
        <LegendItem label="Blue Force"><FriendFrame /></LegendItem>
        <LegendItem label="Red Force">
          <polygon points="9,1 15,7 9,13 3,7" fill={SIDE_FILLS.RED} stroke={INK} strokeWidth="1" />
        </LegendItem>
        <LegendItem label="Зона огляду">
          <circle cx="9" cy="7" r="5.5" fill={SIDE_FILLS.BLUE} fillOpacity="0.2" stroke={SIDE_COLORS.BLUE} strokeDasharray="2 1.5" />
        </LegendItem>
        <LegendItem label="Виявлений противником">
          <circle cx="9" cy="7" r="5.5" fill="none" stroke={SIDE_COLORS.RED} strokeDasharray="1.5 1.5" />
        </LegendItem>
      </LegendGroup>
      <LegendGroup title="Тип">
        <LegendItem label="танковий"><FriendFrame>{track}</FriendFrame></LegendItem>
        <LegendItem label="мех. піхота"><FriendFrame>{cross}{track}</FriendFrame></LegendItem>
        <LegendItem label="піхота"><FriendFrame>{cross}</FriendFrame></LegendItem>
        <LegendItem label="розвідка"><FriendFrame><path d="M2 11.5 L16 2.5" stroke={INK} strokeWidth="1" /></FriendFrame></LegendItem>
        <LegendItem label="невідомий"><FriendFrame /></LegendItem>
      </LegendGroup>
      <LegendGroup title="Стан">
        <LegendItem label="пошкоджений, огляд ×0.5">
          <rect x="2" y="1" width="14" height="7" fill={SIDE_FILLS.BLUE} stroke={INK} strokeWidth="1" />
          <rect x="2" y="10" width="14" height="3" fill={MAP_COLORS.conditionDamaged} stroke={INK} strokeWidth="0.6" />
        </LegendItem>
        <LegendItem label="знищений">
          <rect x="2" y="1" width="14" height="7" fill={MAP_COLORS.destroyedFill} stroke={INK} strokeWidth="1" />
          <rect x="2" y="10" width="14" height="3" fill={MAP_COLORS.conditionDestroyed} stroke={INK} strokeWidth="0.6" />
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
      <p className="border-b border-rule-soft px-3 py-1 text-xs text-ink-soft">{hint}</p>
      <BattlefieldMap battlefield={battlefield} trails={trails} viewMode={viewMode} />
      <Legend />
    </Panel>
  );
}
