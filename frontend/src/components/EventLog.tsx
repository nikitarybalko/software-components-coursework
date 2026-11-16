import { EVENT_META, SIDE_FILLS, TONE_TEXT, cn, formatTime } from "@/lib/presentation";
import type { EventLogEntry } from "@/lib/types";
import { Marker, Panel } from "./ui";

export function EventLog({ events }: { events: EventLogEntry[] }) {
  return (
    <Panel index={5} title="Журнал бойових подій" bodyClassName="p-0"
           aside={<span className="font-mono text-xs text-ink-faint">{events.length} записів</span>}>
      {events.length === 0 ? (
        <p className="px-3 py-3 text-sm text-ink-soft">Записів поки немає.</p>
      ) : (
        <ol className="max-h-80 overflow-y-auto font-mono text-[13px]">
          {events.map((event) => {
            const meta = EVENT_META[event.type] ?? { label: event.type, tone: "neutral" as const };
            return (
              <li key={event.id}
                  className="grid grid-cols-[4.5rem_minmax(0,1fr)] gap-x-3 border-b border-rule-soft px-3 py-1 last:border-b-0 sm:grid-cols-[4.5rem_7.5rem_minmax(0,1fr)]">
                <time className="tabular-nums text-ink-faint" dateTime={event.timestamp}>{formatTime(event.timestamp)}</time>
                <span className={cn("font-heading font-medium uppercase", TONE_TEXT[meta.tone])}>{meta.label}</span>
                <span className="col-span-2 flex min-w-0 items-center gap-1.5 font-sans text-ink sm:col-span-1">
                  {event.side && <Marker color={SIDE_FILLS[event.side]} />}
                  <span className="break-words">{event.message}</span>
                </span>
              </li>
            );
          })}
        </ol>
      )}
    </Panel>
  );
}
