package io.riskstream.risk.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Tunable thresholds for all rules. Persisted in Redis so they can be changed at runtime from
 * the dashboard without a redeploy.
 */
public record RuleConfig(
        int velocityMaxTxns,
        int velocityWindowSec,
        int velocityPoints,
        double spikeMultiplier,
        int spikeMinHistory,
        int spikePoints,
        int travelWindowSec,
        int travelPoints,
        double highAmountThreshold,
        int highAmountPoints,
        int alertThreshold) {

    public static RuleConfig defaults() {
        return new RuleConfig(6, 60, 40, 8.0, 3, 45, 600, 50, 5000.0, 30, 40);
    }

    /** Rejects nonsensical values so a bad dashboard edit can never break scoring. */
    public void validate() {
        require(velocityMaxTxns >= 1, "velocityMaxTxns must be >= 1");
        require(velocityWindowSec >= 1 && velocityWindowSec <= 3600, "velocityWindowSec must be 1..3600");
        require(spikeMultiplier > 1.0, "spikeMultiplier must be > 1");
        require(spikeMinHistory >= 1, "spikeMinHistory must be >= 1");
        require(travelWindowSec >= 1 && travelWindowSec <= 86_400, "travelWindowSec must be 1..86400");
        require(highAmountThreshold > 0, "highAmountThreshold must be > 0");
        require(alertThreshold >= 1 && alertThreshold <= 100, "alertThreshold must be 1..100");
        require(velocityPoints >= 0 && spikePoints >= 0 && travelPoints >= 0 && highAmountPoints >= 0,
                "points must be >= 0");
    }

    public Map<String, String> toMap() {
        Map<String, String> m = new HashMap<>();
        m.put("velocityMaxTxns", String.valueOf(velocityMaxTxns));
        m.put("velocityWindowSec", String.valueOf(velocityWindowSec));
        m.put("velocityPoints", String.valueOf(velocityPoints));
        m.put("spikeMultiplier", String.valueOf(spikeMultiplier));
        m.put("spikeMinHistory", String.valueOf(spikeMinHistory));
        m.put("spikePoints", String.valueOf(spikePoints));
        m.put("travelWindowSec", String.valueOf(travelWindowSec));
        m.put("travelPoints", String.valueOf(travelPoints));
        m.put("highAmountThreshold", String.valueOf(highAmountThreshold));
        m.put("highAmountPoints", String.valueOf(highAmountPoints));
        m.put("alertThreshold", String.valueOf(alertThreshold));
        return m;
    }

    /** Reads a config from a Redis hash; any missing or unparsable field falls back to its default. */
    public static RuleConfig fromMap(Map<String, String> m) {
        RuleConfig d = defaults();
        if (m == null || m.isEmpty()) {
            return d;
        }
        try {
            RuleConfig parsed = new RuleConfig(
                    intOf(m, "velocityMaxTxns", d.velocityMaxTxns),
                    intOf(m, "velocityWindowSec", d.velocityWindowSec),
                    intOf(m, "velocityPoints", d.velocityPoints),
                    doubleOf(m, "spikeMultiplier", d.spikeMultiplier),
                    intOf(m, "spikeMinHistory", d.spikeMinHistory),
                    intOf(m, "spikePoints", d.spikePoints),
                    intOf(m, "travelWindowSec", d.travelWindowSec),
                    intOf(m, "travelPoints", d.travelPoints),
                    doubleOf(m, "highAmountThreshold", d.highAmountThreshold),
                    intOf(m, "highAmountPoints", d.highAmountPoints),
                    intOf(m, "alertThreshold", d.alertThreshold));
            parsed.validate();
            return parsed;
        } catch (IllegalArgumentException e) {
            return d;
        }
    }

    private static int intOf(Map<String, String> m, String key, int fallback) {
        String v = m.get(key);
        return v == null ? fallback : Integer.parseInt(v.trim());
    }

    private static double doubleOf(Map<String, String> m, String key, double fallback) {
        String v = m.get(key);
        return v == null ? fallback : Double.parseDouble(v.trim());
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new IllegalArgumentException(message);
        }
    }
}
