import { SIDE_FILLS } from "@/lib/presentation";
import type { Battlefield, Side, Unit } from "@/lib/types";
import { Marker, Panel } from "./ui";

interface ForceRow {
  side: Side;
  units: Unit[];
  contacts: number;
}

function count(units: Unit[], status: Unit["status"]) {
  return units.filter((unit) => unit.status === status).length;
}

const COLUMNS = ["Боєзд.", "Пошк.", "Знищ.", "Бачить"];

export function ForceSummary({ battlefield }: { battlefield: Battlefield | null }) {
  const rows: ForceRow[] = [
    { side: "BLUE", units: battlefield?.blueUnits ?? [], contacts: battlefield?.blueVisibleRed.length ?? 0 },
    { side: "RED", units: battlefield?.redUnits ?? [], contacts: battlefield?.redVisibleBlue.length ?? 0 },
  ];

  return (
    <Panel index={3} title="Зведення сил" bodyClassName="p-0">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-rule font-heading text-[11px] uppercase text-ink-faint">
            <th scope="col" className="px-3 py-1.5 text-left font-medium">Сторона</th>
            {COLUMNS.map((column) => (
              <th key={column} scope="col" className="px-2 py-1.5 text-right font-medium">{column}</th>
            ))}
          </tr>
        </thead>
        <tbody className="font-mono tabular-nums">
          {rows.map((row) => (
            <tr key={row.side} className="border-b border-rule-soft last:border-b-0">
              <th scope="row" className="px-3 py-1.5 text-left font-sans font-medium">
                <span className="flex items-center gap-1.5">
                  <Marker color={SIDE_FILLS[row.side]} />
                  {row.side}
                </span>
              </th>
              <td className="px-2 py-1.5 text-right">{count(row.units, "HEALTHY")}</td>
              <td className="px-2 py-1.5 text-right text-caution">{count(row.units, "DAMAGED")}</td>
              <td className="px-2 py-1.5 text-right text-alert">{count(row.units, "DESTROYED")}</td>
              <td className="px-3 py-1.5 text-right">{row.contacts}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Panel>
  );
}
