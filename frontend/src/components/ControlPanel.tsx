"use client";

import { Pause, Play, Power, RotateCcw, SlidersHorizontal } from "lucide-react";
import { useState } from "react";
import type { SimulationStatusInfo } from "@/lib/types";
import { Button, ConfirmButton, Panel } from "./ui";

export type RunAction = (key: string, action: () => Promise<unknown>) => Promise<void>;

export function ControlPanel({ status, busyAction, error, onAction, actions }: {
  status: SimulationStatusInfo | null;
  busyAction: string | null;
  error: string | null;
  onAction: RunAction;
  actions: {
    init: () => Promise<unknown>;
    pause: (reason: string) => Promise<unknown>;
    resume: () => Promise<unknown>;
    reset: () => Promise<unknown>;
  };
}) {
  const [pauseReason, setPauseReason] = useState("");
  const current = status?.status;
  const offline = status === null;
  const sessionInProgress = current === "ACTIVE" || current === "PAUSED";

  return (
    <Panel title="Керування симуляцією" icon={<SlidersHorizontal className="h-4 w-4" aria-hidden />}>
      <div className="grid grid-cols-2 gap-2">
        {sessionInProgress ? (
          <ConfirmButton variant="primary" icon={<Power className="h-4 w-4" aria-hidden />} disabled={offline}
                         busy={busyAction === "init"} confirmLabel="Почати заново?"
                         onConfirm={() => onAction("init", actions.init)}>
            Ініціалізувати
          </ConfirmButton>
        ) : (
          <Button variant="primary" icon={<Power className="h-4 w-4" aria-hidden />} disabled={offline}
                  busy={busyAction === "init"} onClick={() => onAction("init", actions.init)}>
            Ініціалізувати
          </Button>
        )}
        <ConfirmButton icon={<RotateCcw className="h-4 w-4" aria-hidden />} disabled={offline}
                       busy={busyAction === "reset"} confirmLabel="Стерти все?"
                       onConfirm={() => onAction("reset", actions.reset)}>
          Скинути
        </ConfirmButton>
        <Button icon={<Pause className="h-4 w-4" aria-hidden />} disabled={current !== "ACTIVE"}
                busy={busyAction === "pause"}
                onClick={() => onAction("pause", () => actions.pause(pauseReason.trim() || "Пауза з Дашборду Командира"))}>
          Пауза
        </Button>
        <Button icon={<Play className="h-4 w-4" aria-hidden />} disabled={current !== "PAUSED"}
                busy={busyAction === "resume"} onClick={() => onAction("resume", actions.resume)}>
          Продовжити
        </Button>
      </div>

      <label className="mt-3 block text-xs text-slate-400" htmlFor="pause-reason">Причина паузи (необов'язково)</label>
      <input id="pause-reason" value={pauseReason} onChange={(event) => setPauseReason(event.target.value)}
             placeholder="Тактична нарада" maxLength={200}
             className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-600 focus:border-sky-500 focus:outline-none" />

      <p className="mt-3 text-xs leading-relaxed text-slate-500">
        «Скинути» очищує стан у пам&apos;яті, snapshot&apos;и та журнал подій у БД. Якщо симулятор мовчить довше за
        тайм-аут, брокер сам ставить симуляцію на паузу.
      </p>

      {error && (
        <p role="alert" className="mt-3 rounded-lg border border-rose-900 bg-rose-950/60 px-3 py-2 text-xs text-rose-200">
          {error}
        </p>
      )}
    </Panel>
  );
}
