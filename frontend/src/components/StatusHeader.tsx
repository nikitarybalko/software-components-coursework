import type { ReactNode } from "react";
import { MAP_COLORS, SIDE_FILLS, SIDE_LABELS, STATUS_LABELS, STATUS_TONES, TONE_TEXT, cn, formatTick } from "@/lib/presentation";
import type { Side, SimulationStatusInfo } from "@/lib/types";
import { Marker } from "./ui";

const SIDES: Side[] = ["BLUE", "RED"];

function Cell({ label, children, className }: { label: string; children: ReactNode; className?: string }) {
  return (
    <div className={cn("bg-sheet px-3 py-2", className)}>
      <p className="font-heading text-[11px] font-medium uppercase text-ink-faint">{label}</p>
      <div className="mt-0.5">{children}</div>
    </div>
  );
}

function simulatorState(side: Side, status: SimulationStatusInfo | null) {
  if (!status?.registeredSides.includes(side)) {
    return { text: "не зареєстровано", className: "text-ink-faint" };
  }
  if (status.submittedSides.includes(side)) {
    return { text: "хід надіслано", className: "text-condition-ok" };
  }
  if (status.status === "ACTIVE") {
    return { text: "очікуємо хід", className: "text-caution" };
  }
  return { text: "на зв'язку", className: "text-ink" };
}

export function StatusHeader({ status, connectionError, lastUpdate }: {
  status: SimulationStatusInfo | null;
  connectionError: string | null;
  lastUpdate: Date | null;
}) {
  const connected = status !== null && connectionError === null;

  return (
    <header className="grid grid-cols-2 gap-px border border-ink bg-ink sm:grid-cols-3 xl:grid-cols-[minmax(0,2.2fr)_repeat(5,minmax(0,1fr))]">
      <Cell label="Аркуш обстановки" className="col-span-2 sm:col-span-3 xl:col-span-1">
        <h1 className="font-heading text-xl font-semibold uppercase leading-tight text-ink">Дашборд Командира</h1>
        <p className="text-xs text-ink-soft">Wargame Broker · спільна обстановка Blue та Red Force</p>
      </Cell>

      <Cell label="Тік">
        <p className="font-mono text-2xl font-medium leading-none tabular-nums">{formatTick(status?.currentTick)}</p>
      </Cell>

      <Cell label="Сесія">
        <p className={cn("text-sm font-medium", status ? TONE_TEXT[STATUS_TONES[status.status]] : "text-ink-faint")}>
          {status ? STATUS_LABELS[status.status] : "невідомо"}
        </p>
      </Cell>

      {SIDES.map((side) => {
        const state = simulatorState(side, status);
        return (
          <Cell key={side} label={SIDE_LABELS[side]}>
            <p className="flex items-center gap-1.5 text-sm">
              <Marker color={SIDE_FILLS[side]} />
              <span className={state.className}>{state.text}</span>
            </p>
          </Cell>
        );
      })}

      <Cell label="Зв'язок з брокером" className="col-span-2 xl:col-span-1">
        <p className="flex items-center gap-1.5 text-sm">
          <Marker color={connected ? "var(--condition-ok)" : MAP_COLORS.conditionDestroyed} />
          {connected ? "є" : "немає"}
          {lastUpdate && (
            <span className="font-mono text-xs text-ink-faint">{lastUpdate.toLocaleTimeString("uk-UA", { hour12: false })}</span>
          )}
        </p>
      </Cell>
    </header>
  );
}
