import { useCallback, useState } from "react";
import { brokerApi } from "@/lib/api";
import type { Battlefield, EventLogEntry, SimulationStatusInfo, SnapshotInfo } from "@/lib/types";
import { usePolling } from "./usePolling";

const LIVE_INTERVAL_MS = 700;
const HISTORY_INTERVAL_MS = 2500;

export interface BrokerState {
  status: SimulationStatusInfo | null;
  battlefield: Battlefield | null;
  snapshots: SnapshotInfo[];
  events: EventLogEntry[];
  connectionError: string | null;
  lastUpdate: Date | null;
  refresh: () => Promise<void>;
}

export function useBrokerState(): BrokerState {
  const [status, setStatus] = useState<SimulationStatusInfo | null>(null);
  const [battlefield, setBattlefield] = useState<Battlefield | null>(null);
  const [snapshots, setSnapshots] = useState<SnapshotInfo[]>([]);
  const [events, setEvents] = useState<EventLogEntry[]>([]);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  const refreshLive = useCallback(async () => {
    try {
      const [nextStatus, nextBattlefield] = await Promise.all([brokerApi.status(), brokerApi.battlefield()]);
      setStatus(nextStatus);
      setBattlefield(nextBattlefield);
      setConnectionError(null);
      setLastUpdate(new Date());
    } catch (error) {
      setConnectionError(error instanceof Error ? error.message : "Брокер недоступний");
    }
  }, []);

  const refreshHistory = useCallback(async () => {
    const [nextSnapshots, nextEvents] = await Promise.all([brokerApi.snapshots(), brokerApi.events()]);
    setSnapshots(nextSnapshots);
    setEvents(nextEvents);
  }, []);

  usePolling(refreshLive, LIVE_INTERVAL_MS);
  usePolling(refreshHistory, HISTORY_INTERVAL_MS);

  const refresh = useCallback(async () => {
    await Promise.all([refreshLive(), refreshHistory().catch(() => undefined)]);
  }, [refreshLive, refreshHistory]);

  return { status, battlefield, snapshots, events, connectionError, lastUpdate, refresh };
}
