import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { EventType, Side, SimulationStatus, Unit, UnitStatus, UnitType } from "./types";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Map palette; mirrors the CSS variables in globals.css because SVG attributes need literal colors. */
export const MAP_COLORS = {
  ink: "#1c2318",
  inkSoft: "#4a5443",
  inkFaint: "#858a76",
  sheet: "#f4f0e3",
  rule: "#b3ab93",
  ruleSoft: "#d6cfb8",
  destroyedFill: "#c9c2ad",
  conditionDamaged: "#d9a200",
  conditionDestroyed: "#b0281f",
} as const;

/** APP-6 colour fill for frames. */
export const SIDE_FILLS: Record<Side, string> = {
  BLUE: "#80e0ff",
  RED: "#ff8080",
};

/** Darker side colour for lines drawn on paper: zones, trails, detection rings. */
export const SIDE_COLORS: Record<Side, string> = {
  BLUE: "#1d5d99",
  RED: "#b0281f",
};

export const SIDE_LABELS: Record<Side, string> = {
  BLUE: "Blue Force",
  RED: "Red Force",
};

export const STATUS_LABELS: Record<SimulationStatus, string> = {
  INITIALIZING: "очікує ініціалізації",
  ACTIVE: "активна",
  PAUSED: "пауза",
  STOPPED: "зупинена",
};

export const UNIT_STATUS_LABELS: Record<UnitStatus, string> = {
  HEALTHY: "боєздатний",
  DAMAGED: "пошкоджений",
  DESTROYED: "знищений",
};

export const UNIT_TYPE_LABELS: Record<UnitType, string> = {
  ARMOR: "танковий",
  MECHANIZED_INFANTRY: "механізована піхота",
  INFANTRY: "піхота",
  RECON: "розвідка",
  UNKNOWN: "тип невідомий",
};

export type Tone = "neutral" | "info" | "success" | "warning" | "danger";

export const STATUS_TONES: Record<SimulationStatus, Tone> = {
  INITIALIZING: "neutral",
  ACTIVE: "success",
  PAUSED: "warning",
  STOPPED: "danger",
};

export const EVENT_META: Record<EventType, { label: string; tone: Tone }> = {
  UNIT_DESTROYED: { label: "знищення", tone: "danger" },
  UNIT_DAMAGED: { label: "ураження", tone: "warning" },
  VALIDATION_ERROR: { label: "валідація", tone: "danger" },
  TIMEOUT_DISCONNECT: { label: "тайм-аут", tone: "danger" },
  SIMULATOR_REGISTERED: { label: "реєстрація", tone: "neutral" },
  SIMULATION_INITIALIZED: { label: "старт", tone: "success" },
  SIMULATION_PAUSED: { label: "пауза", tone: "warning" },
  SIMULATION_RESUMED: { label: "продовження", tone: "success" },
  SIMULATION_RESET: { label: "скидання", tone: "info" },
  SNAPSHOT_SAVED: { label: "контр. точка", tone: "neutral" },
  SNAPSHOT_RESTORED: { label: "відновлення", tone: "info" },
};

export const TONE_TEXT: Record<Tone, string> = {
  neutral: "text-ink-soft",
  info: "text-friend",
  success: "text-condition-ok",
  warning: "text-caution",
  danger: "text-alert",
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

export function formatTick(tick: number | undefined) {
  return tick === undefined ? "———" : String(tick).padStart(3, "0");
}
