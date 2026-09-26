import { useState } from "react";
import { api } from "../api";
import type { FraudPattern, ProducerStatus } from "../types";

const PATTERNS: { id: FraudPattern; label: string; hint: string }[] = [
  { id: "velocity", label: "Velocity burst", hint: "12 rapid purchases on one card" },
  { id: "travel", label: "Impossible travel", hint: "two countries within seconds" },
  { id: "spike", label: "Amount spike", hint: "one purchase ~30x the card's norm" },
];

export function ControlPanel({ producer }: { producer: ProducerStatus | null }) {
  const [rate, setRate] = useState(20);
  const [message, setMessage] = useState<string | null>(null);

  const run = async (action: () => Promise<string>) => {
    try {
      setMessage(await action());
    } catch (e) {
      setMessage((e as Error).message);
    }
  };

  return (
    <section className="card">
      <h2>Traffic &amp; fraud injection</h2>

      <label className="field">
        <span>
          Steady traffic: <strong>{rate}</strong> txn/s
          {producer && <em className="muted"> (running at {producer.tps})</em>}
        </span>
        <input type="range" min={0} max={500} step={5} value={rate} onChange={(e) => setRate(Number(e.target.value))} />
      </label>
      <button className="btn" onClick={() => run(async () => `Rate set to ${(await api.setRate(rate)).tps} txn/s`)}>
        Apply rate
      </button>

      <h3>Inject fraud</h3>
      <div className="inject">
        {PATTERNS.map((p) => (
          <button
            key={p.id}
            className="btn btn-danger"
            title={p.hint}
            onClick={() => run(async () => `Injected ${p.label} on ${(await api.inject(p.id)).cardId}`)}
          >
            {p.label}
          </button>
        ))}
      </div>

      {message && <p className="notice">{message}</p>}
    </section>
  );
}
