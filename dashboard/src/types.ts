export type Severity = "MEDIUM" | "HIGH" | "CRITICAL";

export interface Alert {
  alertId: string;
  txnId: string;
  cardId: string;
  merchant: string;
  amount: number;
  currency: string;
  country: string;
  score: number;
  severity: Severity;
  reasons: string[];
  txnTimestamp: number;
  raisedAt: number;
  endToEndLatencyMs: number;
}

export interface RuleConfig {
  velocityMaxTxns: number;
  velocityWindowSec: number;
  velocityPoints: number;
  spikeMultiplier: number;
  spikeMinHistory: number;
  spikePoints: number;
  travelWindowSec: number;
  travelPoints: number;
  highAmountThreshold: number;
  highAmountPoints: number;
  alertThreshold: number;
}

export interface Stats {
  processed: number;
  duplicates: number;
  alerts: number;
  scoringMeanMs: number;
  scoringP50Ms?: number;
  scoringP95Ms?: number;
  scoringP99Ms?: number;
  uptimeSec: number;
}

export interface ProducerStatus {
  tps: number;
  sent: number;
  failed: number;
}

export type FraudPattern = "velocity" | "travel" | "spike";
