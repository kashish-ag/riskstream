import type { Alert, FraudPattern, ProducerStatus, RuleConfig, Stats } from "./types";

async function json<T>(res: Response): Promise<T> {
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error((body as { error?: string }).error ?? `Request failed (${res.status})`);
  }
  return body as T;
}

export const api = {
  alerts: () => fetch("/api/alerts").then((r) => json<Alert[]>(r)),
  stats: () => fetch("/api/stats").then((r) => json<Stats>(r)),
  config: () => fetch("/api/config").then((r) => json<RuleConfig>(r)),
  saveConfig: (config: RuleConfig) =>
    fetch("/api/config", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(config),
    }).then((r) => json<RuleConfig>(r)),
  resetConfig: () => fetch("/api/config/reset", { method: "POST" }).then((r) => json<RuleConfig>(r)),
  producerStatus: () => fetch("/api/producer/status").then((r) => json<ProducerStatus>(r)),
  setRate: (tps: number) =>
    fetch(`/api/producer/rate?tps=${tps}`, { method: "POST" }).then((r) => json<{ tps: number }>(r)),
  inject: (pattern: FraudPattern) =>
    fetch(`/api/producer/fraud/${pattern}`, { method: "POST" }).then((r) =>
      json<{ injected: number; cardId: string }>(r),
    ),
};
