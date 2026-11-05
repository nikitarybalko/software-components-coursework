import { CheckCircle2, Clock, Radar, Wifi, WifiOff } from "lucide-react";
import { SIDE_COLORS, SIDE_LABELS, STATUS_LABELS, STATUS_TONES, cn } from "@/lib/presentation";
import type { Side, SimulationStatusInfo } from "@/lib/types";
import { Badge } from "./ui";

const SIDES: Side[] = ["BLUE", "RED"];

function SimulatorChip({ side, status }: { side: Side; status: SimulationStatusInfo | null }) {
  const registered = status?.registeredSides.includes(side) ?? false;
  const submitted = status?.submittedSides.includes(side) ?? false;
  const waitingForTurn = registered && status?.status === "ACTIVE" && !submitted;

  return (
    <div className="flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-900/80 px-3 py-2">
      <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: SIDE_COLORS[side] }} aria-hidden />
      <div className="leading-tight">
        <p className="text-xs font-semibold text-slate-200">{SIDE_LABELS[side]}</p>
        <p className={cn("flex items-center gap-1 text-[11px]", registered ? "text-slate-400" : "text-slate-500")}>
          {!registered && "не зареєстровано"}
          {submitted && (
            <>
              <CheckCircle2 className="h-3 w-3 text-emerald-400" aria-hidden /> хід надіслано
            </>
          )}
          {waitingForTurn && (
            <>
              <Clock className="h-3 w-3" aria-hidden /> очікуємо хід
            </>
          )}
          {registered && !submitted && !waitingForTurn && "зареєстровано"}
        </p>
      </div>
    </div>
  );
}

export function StatusHeader({ status, connectionError }: {
  status: SimulationStatusInfo | null;
  connectionError: string | null;
}) {
  const connected = status !== null && connectionError === null;

  return (
    <header className="flex flex-col gap-4 rounded-xl border border-slate-800 bg-slate-900/70 p-4 lg:flex-row lg:items-center lg:justify-between">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-sky-500/15 text-sky-400">
          <Radar className="h-5 w-5" aria-hidden />
        </div>
        <div>
          <h1 className="text-lg font-semibold text-slate-50">Дашборд Командира</h1>
          <p className="flex items-center gap-1.5 text-xs text-slate-400">
            {connected ? <Wifi className="h-3.5 w-3.5 text-emerald-400" aria-hidden /> : <WifiOff className="h-3.5 w-3.5 text-rose-400" aria-hidden />}
            {connected ? "Wargame Broker на зв'язку" : "Немає зв'язку з брокером"}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="rounded-lg border border-slate-800 bg-slate-900/80 px-3 py-1.5 text-center">
          <p className="text-[11px] uppercase tracking-wide text-slate-500">Тік</p>
          <p className="font-mono text-xl font-semibold tabular-nums text-slate-50">{status?.currentTick ?? "—"}</p>
        </div>
        {status && (
          <Badge tone={STATUS_TONES[status.status]} className="px-3 py-1.5 text-sm">
            {STATUS_LABELS[status.status]}
          </Badge>
        )}
        {SIDES.map((side) => <SimulatorChip key={side} side={side} status={status} />)}
      </div>
    </header>
  );
}
