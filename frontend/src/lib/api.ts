import type { Battlefield, EventLogEntry, SimulationStatusInfo, SnapshotInfo } from "./types";

const BASE_PATH = "/api/simulation";

export class BrokerError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_PATH}/${path}`, { cache: "no-store", ...init });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new BrokerError(body?.message ?? `HTTP ${response.status}`, response.status);
  }
  return body as T;
}

function command(path: string, payload?: unknown): Promise<SimulationStatusInfo> {
  return request<SimulationStatusInfo>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: payload === undefined ? undefined : JSON.stringify(payload),
  });
}

export const brokerApi = {
  status: () => request<SimulationStatusInfo>("status"),
  battlefield: () => request<Battlefield>("state/full"),
  snapshots: () => request<SnapshotInfo[]>("state/snapshots"),
  events: () => request<EventLogEntry[]>("state/events"),
  init: () => command("init"),
  pause: (reason: string) => command("pause", { reason }),
  resume: () => command("resume"),
  reset: () => command("reset"),
  restore: (snapshotId: string) => command(`restore/${snapshotId}`),
};
