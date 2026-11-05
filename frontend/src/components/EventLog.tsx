import { ScrollText } from "lucide-react";
import { EVENT_META, SIDE_COLORS, formatTime } from "@/lib/presentation";
import type { EventLogEntry } from "@/lib/types";
import { Badge, Panel } from "./ui";

export function EventLog({ events }: { events: EventLogEntry[] }) {
  return (
    <Panel title="Журнал подій" icon={<ScrollText className="h-4 w-4" aria-hidden />}
           actions={<span className="text-xs text-slate-500">останні {events.length}</span>}>
      {events.length === 0 ? (
        <p className="text-sm text-slate-500">Подій поки немає.</p>
      ) : (
        <ol className="-mr-2 max-h-80 divide-y divide-slate-800/70 overflow-y-auto pr-2">
          {events.map((event) => {
            const meta = EVENT_META[event.type] ?? { label: event.type, tone: "neutral" as const };
            return (
              <li key={event.id} className="flex flex-col gap-1 py-2 sm:flex-row sm:items-center sm:gap-3">
                <div className="flex shrink-0 items-center gap-2">
                  <time className="font-mono text-xs tabular-nums text-slate-500" dateTime={event.timestamp}>
                    {formatTime(event.timestamp)}
                  </time>
                  <Badge tone={meta.tone} className="w-[7.5rem] justify-center">{meta.label}</Badge>
                </div>
                <p className="flex min-w-0 items-center gap-2 text-sm text-slate-300">
                  {event.side && (
                    <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: SIDE_COLORS[event.side] }} aria-hidden />
                  )}
                  <span className="break-words">{event.message}</span>
                </p>
              </li>
            );
          })}
        </ol>
      )}
    </Panel>
  );
}
