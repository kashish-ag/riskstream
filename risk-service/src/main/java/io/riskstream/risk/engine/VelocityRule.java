package io.riskstream.risk.engine;

import io.riskstream.common.Transaction;
import io.riskstream.risk.store.RiskStateStore;
import java.util.Optional;

/** Too many transactions on one card inside a sliding time window. */
public class VelocityRule implements Rule {

    private final RiskStateStore store;

    public VelocityRule(RiskStateStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "VELOCITY";
    }

    @Override
    public Optional<RuleHit> evaluate(Transaction txn, RuleConfig cfg) {
        long windowMs = cfg.velocityWindowSec() * 1000L;
        long count = store.recordAndCount(txn.cardId(), txn.id(), txn.timestamp(), windowMs);
        if (count > cfg.velocityMaxTxns()) {
            return Optional.of(new RuleHit(name(), cfg.velocityPoints(),
                    "Velocity: " + count + " transactions in " + cfg.velocityWindowSec()
                            + "s (limit " + cfg.velocityMaxTxns() + ")"));
        }
        return Optional.empty();
    }
}
