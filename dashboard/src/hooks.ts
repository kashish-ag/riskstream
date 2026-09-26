import { useEffect, useRef, useState } from "react";
import { api } from "./api";
import type { Alert, ProducerStatus, Stats } from "./types";

const MAX_ALERTS = 200;

/** Loads recent alerts, then streams new ones over Server-Sent Events. */
export function useAlerts() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    let cancelled = false;
    api
      .alerts()
      .then((initial) => {
        if (!cancelled) setAlerts(initial.slice(0, MAX_ALERTS));
      })
      .catch(() => undefined);

    const source = new EventSource("/api/alerts/stream");
    source.onopen = () => setConnected(true);
    source.onerror = () => setConnected(false); // EventSource reconnects on its own
    source.addEventListener("alert", (event) => {
      const alert = JSON.parse((event as MessageEvent<string>).data) as Alert;
      setAlerts((prev) => [alert, ...prev.filter((a) => a.alertId !== alert.alertId)].slice(0, MAX_ALERTS));
    });

    return () => {
      cancelled = true;
      source.close();
    };
  }, []);

  return { alerts, connected };
}

/** Polls stats once a second and derives throughput (transactions/second) from the counter delta. */
export function useStats() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [producer, setProducer] = useState<ProducerStatus | null>(null);
  const [throughput, setThroughput] = useState(0);
  const last = useRef<{ processed: number; at: number } | null>(null);

  useEffect(() => {
    const tick = () => {
      api
        .stats()
        .then((s) => {
          const now = Date.now();
          if (last.current) {
            const seconds = (now - last.current.at) / 1000;
            if (seconds > 0) setThroughput(Math.max(0, (s.processed - last.current.processed) / seconds));
          }
          last.current = { processed: s.processed, at: now };
          setStats(s);
        })
        .catch(() => setStats(null));
      api
        .producerStatus()
        .then(setProducer)
        .catch(() => setProducer(null));
    };
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, []);

  return { stats, producer, throughput };
}
