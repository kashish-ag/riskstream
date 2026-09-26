package io.riskstream.risk.engine;

import io.riskstream.risk.store.RiskStateStore;

/**
 * Serves the live rule configuration. Reads go through a short in-process cache (1 s) so the
 * hot path does not hit Redis for config on every transaction, while dashboard edits still
 * take effect almost immediately on every service instance.
 */
public class RuleConfigService {

    private static final long CACHE_MS = 1_000;

    private final RiskStateStore store;
    private volatile RuleConfig cached;
    private volatile long loadedAt;

    public RuleConfigService(RiskStateStore store) {
        this.store = store;
    }

    public RuleConfig current() {
        long now = System.currentTimeMillis();
        RuleConfig config = cached;
        if (config == null || now - loadedAt > CACHE_MS) {
            config = RuleConfig.fromMap(store.readConfig());
            cached = config;
            loadedAt = now;
        }
        return config;
    }

    /** Validates and persists a new configuration. Throws IllegalArgumentException if invalid. */
    public RuleConfig update(RuleConfig next) {
        next.validate();
        store.writeConfig(next.toMap());
        cached = next;
        loadedAt = System.currentTimeMillis();
        return next;
    }

    public RuleConfig reset() {
        store.clearConfig();
        RuleConfig defaults = RuleConfig.defaults();
        cached = defaults;
        loadedAt = System.currentTimeMillis();
        return defaults;
    }
}
