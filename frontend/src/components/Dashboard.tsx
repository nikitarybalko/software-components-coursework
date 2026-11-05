"use client";

import { useCallback, useState } from "react";
import { useBrokerState } from "@/hooks/useBrokerState";
import { useUnitTrails } from "@/hooks/useUnitTrails";
import { brokerApi } from "@/lib/api";
import type { ViewMode } from "./BattlefieldMap";
import { BattlefieldPanel } from "./BattlefieldPanel";
import { ControlPanel, type RunAction } from "./ControlPanel";
import { EventLog } from "./EventLog";
import { ForceSummary } from "./ForceSummary";
import { SnapshotList } from "./SnapshotList";
import { StatusHeader } from "./StatusHeader";

export function Dashboard() {
  const { status, battlefield, snapshots, events, connectionError, refresh } = useBrokerState();
  const trails = useUnitTrails(battlefield);
  const [viewMode, setViewMode] = useState<ViewMode>("ARBITER");
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const runAction: RunAction = useCallback(async (key, action) => {
    setBusyAction(key);
    setActionError(null);
    try {
      await action();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Не вдалося виконати команду");
    } finally {
      await refresh();
      setBusyAction(null);
    }
  }, [refresh]);

  return (
    <main className="mx-auto max-w-[1600px] space-y-4 px-4 py-4 sm:px-6 sm:py-6">
      <StatusHeader status={status} connectionError={connectionError} />

      {connectionError && (
        <p role="alert" className="rounded-xl border border-rose-900 bg-rose-950/50 px-4 py-3 text-sm text-rose-200">
          {connectionError}. Дані на екрані можуть бути застарілими.
        </p>
      )}

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
        <div className="min-w-0 space-y-4">
          <BattlefieldPanel battlefield={battlefield} trails={trails} viewMode={viewMode} onViewModeChange={setViewMode} />
          <EventLog events={events} />
        </div>
        <aside className="space-y-4">
          <ControlPanel status={status} busyAction={busyAction} error={actionError} onAction={runAction}
                        actions={brokerApi} />
          <ForceSummary battlefield={battlefield} />
          <SnapshotList snapshots={snapshots} busyAction={busyAction} onAction={runAction} restore={brokerApi.restore} />
        </aside>
      </div>
    </main>
  );
}
