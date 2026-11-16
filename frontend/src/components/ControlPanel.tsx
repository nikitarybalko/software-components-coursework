"use client";

import { useState } from "react";
import type { SimulationStatusInfo } from "@/lib/types";
import { Button, ConfirmButton, Panel } from "./ui";

export type RunAction = (key: string, action: () => Promise<unknown>) => Promise<void>;

const DEFAULT_PAUSE_REASON = "Пауза з Дашборду Командира";

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
  const init = () => onAction("init", actions.init);

  return (
    <Panel index={2} title="Керування">
      <div className="grid grid-cols-2 gap-2">
        {sessionInProgress ? (
          <ConfirmButton variant="primary" disabled={offline} busy={busyAction === "init"}
                         confirmLabel="Почати заново?" onConfirm={init}>
            Ініціалізувати
          </ConfirmButton>
        ) : (
          <Button variant="primary" disabled={offline} busy={busyAction === "init"} onClick={init}>
            Ініціалізувати
          </Button>
        )}
        <ConfirmButton disabled={offline} busy={busyAction === "reset"} confirmLabel="Стерти все?"
                       onConfirm={() => onAction("reset", actions.reset)}>
          Скинути
        </ConfirmButton>
        <Button disabled={current !== "ACTIVE"} busy={busyAction === "pause"}
                onClick={() => onAction("pause", () => actions.pause(pauseReason.trim() || DEFAULT_PAUSE_REASON))}>
          Пауза
        </Button>
        <Button disabled={current !== "PAUSED"} busy={busyAction === "resume"}
                onClick={() => onAction("resume", actions.resume)}>
          Продовжити
        </Button>
      </div>

      <label className="mt-3 block font-heading text-[11px] font-medium uppercase text-ink-faint" htmlFor="pause-reason">
        Причина паузи
      </label>
      <input id="pause-reason" value={pauseReason} onChange={(event) => setPauseReason(event.target.value)}
             placeholder="Тактична нарада" maxLength={200}
             className="mt-1 w-full border border-rule bg-paper px-2 py-1.5 text-sm text-ink placeholder:text-ink-faint focus:border-ink focus:outline-none" />

      <p className="mt-3 border-t border-rule-soft pt-2 text-xs leading-relaxed text-ink-soft">
        «Скинути» стирає стан у пам&apos;яті, контрольні точки та журнал. Якщо симулятор мовчить довше за тайм-аут,
        брокер сам ставить сесію на паузу.
      </p>

      {error && (
        <p role="alert" className="mt-2 border border-alert px-2 py-1.5 text-xs text-alert">
          {error}
        </p>
      )}
    </Panel>
  );
}
