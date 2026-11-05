import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { EventType, Side, SimulationStatus, Unit, UnitStatus } from "./types";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const SIDE_COLORS: Record<Side, string> = {
  BLUE: "#60a5fa",
  RED: "#f87171",
};

export const DAMAGED_COLOR = "#fbbf24";
export const DESTROYED_COLOR = "#64748b";

export const SIDE_LABELS: Record<Side, string> = {
  BLUE: "Blue Force",
  RED: "Red Force",
};

export const STATUS_LABELS: Record<SimulationStatus, string> = {
  INITIALIZING: "Очікує ініціалізації",
  ACTIVE: "Активна",
  PAUSED: "Пауза",
  STOPPED: "Зупинена",
};

export const UNIT_STATUS_LABELS: Record<UnitStatus, string> = {
  HEALTHY: "боєздатний",
  DAMAGED: "пошкоджений",
  DESTROYED: "знищений",
};

export type Tone = "neutral" | "info" | "success" | "warning" | "danger";

export const STATUS_TONES: Record<SimulationStatus, Tone> = {
  INITIALIZING: "neutral",
  ACTIVE: "success",
  PAUSED: "warning",
  STOPPED: "danger",
};

export const EVENT_META: Record<EventType, { label: string; tone: Tone }> = {
  UNIT_DESTROYED: { label: "Знищення", tone: "danger" },
  UNIT_DAMAGED: { label: "Пошкодження", tone: "warning" },
  VALIDATION_ERROR: { label: "Валідація", tone: "danger" },
  TIMEOUT_DISCONNECT: { label: "Тайм-аут", tone: "danger" },
  SIMULATOR_REGISTERED: { label: "Реєстрація", tone: "success" },
  SIMULATION_INITIALIZED: { label: "Старт", tone: "success" },
  SIMULATION_PAUSED: { label: "Пауза", tone: "warning" },
  SIMULATION_RESUMED: { label: "Продовження", tone: "success" },
  SIMULATION_RESET: { label: "Скидання", tone: "info" },
  SNAPSHOT_SAVED: { label: "Snapshot", tone: "neutral" },
  SNAPSHOT_RESTORED: { label: "Відновлення", tone: "info" },
};

export const TONE_CLASSES: Record<Tone, string> = {
  neutral: "border-slate-700 bg-slate-800/60 text-slate-300",
  info: "border-sky-800 bg-sky-950/60 text-sky-300",
  success: "border-emerald-800 bg-emerald-950/60 text-emerald-300",
  warning: "border-amber-800 bg-amber-950/60 text-amber-300",
  danger: "border-rose-800 bg-rose-950/60 text-rose-300",
};

export function unitKey(unit: Pick<Unit, "side" | "id">) {
  return `${unit.side}:${unit.id}`;
}

export function shortUnitName(unit: Pick<Unit, "id">) {
  return unit.id.replace(/^(blue|red)-/i, "");
}

export function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString("uk-UA", { hour12: false });
}
