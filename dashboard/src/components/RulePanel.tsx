import { useEffect, useState } from "react";
import { api } from "../api";
import type { RuleConfig } from "../types";

const FIELDS: { key: keyof RuleConfig; label: string; step?: number }[] = [
  { key: "velocityMaxTxns", label: "Velocity: max txns" },
  { key: "velocityWindowSec", label: "Velocity: window (s)" },
  { key: "velocityPoints", label: "Velocity: points" },
  { key: "spikeMultiplier", label: "Spike: x card average", step: 0.5 },
  { key: "spikeMinHistory", label: "Spike: min history" },
  { key: "spikePoints", label: "Spike: points" },
  { key: "travelWindowSec", label: "Travel: window (s)" },
  { key: "travelPoints", label: "Travel: points" },
  { key: "highAmountThreshold", label: "High amount: threshold", step: 100 },
  { key: "highAmountPoints", label: "High amount: points" },
  { key: "alertThreshold", label: "Alert at score >=" },
];

export function RulePanel() {
  const [config, setConfig] = useState<RuleConfig | null>(null);
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null);

  useEffect(() => {
    api
      .config()
      .then(setConfig)
      .catch((e: Error) => setMessage({ text: e.message, error: true }));
  }, []);

  if (!config) {
    return (
      <section className="card">
        <h2>Rule tuning</h2>
        <p className="muted">{message?.text ?? "Loading…"}</p>
      </section>
    );
  }

  const save = async () => {
    try {
      setConfig(await api.saveConfig(config));
      setMessage({ text: "Saved. Live on the next transaction.", error: false });
    } catch (e) {
      setMessage({ text: (e as Error).message, error: true });
    }
  };

  const reset = async () => {
    try {
      setConfig(await api.resetConfig());
      setMessage({ text: "Reset to defaults.", error: false });
    } catch (e) {
      setMessage({ text: (e as Error).message, error: true });
    }
  };

  return (
    <section className="card">
      <h2>Rule tuning</h2>
      <p className="muted small">Stored in Redis — changes apply without a redeploy.</p>
      <div className="rule-grid">
        {FIELDS.map((f) => (
          <label className="field-inline" key={f.key}>
            <span>{f.label}</span>
            <input
              type="number"
              step={f.step ?? 1}
              value={config[f.key]}
              onChange={(e) => setConfig({ ...config, [f.key]: Number(e.target.value) })}
            />
          </label>
        ))}
      </div>
      <div className="row">
        <button className="btn" onClick={save}>
          Save
        </button>
        <button className="btn btn-ghost" onClick={reset}>
          Reset
        </button>
      </div>
      {message && <p className={`notice ${message.error ? "notice-error" : ""}`}>{message.text}</p>}
    </section>
  );
}
