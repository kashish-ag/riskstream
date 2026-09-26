import type { Stats } from "../types";

interface Props {
  stats: Stats | null;
  throughput: number;
}

const fmt = (n: number | undefined, digits = 0) =>
  n === undefined ? "–" : n.toLocaleString(undefined, { maximumFractionDigits: digits });

export function StatTiles({ stats, throughput }: Props) {
  const alertRate = stats && stats.processed > 0 ? (stats.alerts / stats.processed) * 100 : undefined;
  const tiles = [
    { label: "Throughput", value: stats ? fmt(throughput, 1) : "–", unit: "txn/s" },
    { label: "Processed", value: fmt(stats?.processed), unit: "txns" },
    { label: "Alerts", value: fmt(stats?.alerts), unit: alertRate === undefined ? "" : `${fmt(alertRate, 2)}% of txns` },
    { label: "Scoring p50", value: fmt(stats?.scoringP50Ms, 2), unit: "ms" },
    { label: "Scoring p99", value: fmt(stats?.scoringP99Ms, 2), unit: "ms" },
    { label: "Duplicates skipped", value: fmt(stats?.duplicates), unit: "idempotent" },
  ];

  return (
    <div className="tiles">
      {tiles.map((t) => (
        <div className="tile" key={t.label}>
          <div className="tile-label">{t.label}</div>
          <div className="tile-value">{t.value}</div>
          <div className="tile-unit">{t.unit || " "}</div>
        </div>
      ))}
    </div>
  );
}
