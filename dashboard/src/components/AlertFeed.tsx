import type { Alert } from "../types";

const time = (ms: number) => new Date(ms).toLocaleTimeString();

export function AlertFeed({ alerts }: { alerts: Alert[] }) {
  if (alerts.length === 0) {
    return (
      <p className="empty">
        No alerts yet. Use <strong>Inject fraud</strong> on the right to trigger one.
      </p>
    );
  }

  return (
    <ul className="feed">
      {alerts.map((a) => (
        <li key={a.alertId} className={`alert alert-${a.severity.toLowerCase()}`}>
          <div className="alert-top">
            <span className={`badge badge-${a.severity.toLowerCase()}`}>{a.severity}</span>
            <span className="alert-card">{a.cardId}</span>
            <span className="alert-amount">
              {a.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })} {a.currency}
            </span>
            <span className="alert-meta">
              {a.merchant} · {a.country}
            </span>
            <span className="alert-score">score {a.score}</span>
            <span className="alert-time">{time(a.raisedAt)}</span>
          </div>
          <ul className="reasons">
            {a.reasons.map((r) => (
              <li key={r}>{r}</li>
            ))}
          </ul>
        </li>
      ))}
    </ul>
  );
}
