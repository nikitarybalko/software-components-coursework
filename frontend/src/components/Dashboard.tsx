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
  const { status, battlefield, snapshots, events, connectionError, lastUpdate, refresh } = useBrokerState();
  const trails = useUnitTrails(battlefield, status?.epoch);
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
    <main className="mx-auto max-w-[1600px] space-y-3 px-4 py-4 sm:px-6 sm:py-5">
      <StatusHeader status={status} connectionError={connectionError} lastUpdate={lastUpdate} />

      {connectionError && (
        <p role="alert" className="border border-alert bg-sheet px-3 py-2 text-sm text-alert">
          {connectionError}. Обстановка на екрані може бути застарілою.
        </p>
      )}

      <div className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="min-w-0 space-y-3">
          <BattlefieldPanel battlefield={battlefield} trails={trails} viewMode={viewMode} onViewModeChange={setViewMode} />
          <EventLog events={events} />
        </div>
        <aside className="space-y-3">
          <ControlPanel status={status} busyAction={busyAction} error={actionError} onAction={runAction}
                        actions={brokerApi} />
          <ForceSummary battlefield={battlefield} />
          <SnapshotList snapshots={snapshots} busyAction={busyAction} onAction={runAction} restore={brokerApi.restore} />
        </aside>
      </div>
    </main>
  );
}
