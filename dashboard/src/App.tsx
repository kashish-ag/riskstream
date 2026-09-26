import { useAlerts, useStats } from "./hooks";
import { AlertFeed } from "./components/AlertFeed";
import { ControlPanel } from "./components/ControlPanel";
import { RulePanel } from "./components/RulePanel";
import { StatTiles } from "./components/StatTiles";

export default function App() {
  const { alerts, connected } = useAlerts();
  const { stats, producer, throughput } = useStats();

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>riskstream</h1>
          <p className="subtitle">Real-time transaction risk engine · Kafka · Redis · Spring Boot</p>
        </div>
        <span className={`pill ${connected ? "pill-ok" : "pill-bad"}`}>
          <span className="dot" /> {connected ? "live" : "reconnecting…"}
        </span>
      </header>

      <StatTiles stats={stats} throughput={throughput} />

      <main className="grid">
        <section className="card card-wide">
          <h2>Live alerts</h2>
          <AlertFeed alerts={alerts} />
        </section>
        <aside className="side">
          <ControlPanel producer={producer} />
          <RulePanel />
        </aside>
      </main>
    </div>
  );
}
