package io.riskstream.risk.engine;

import io.riskstream.common.Transaction;
import io.riskstream.risk.store.RiskStateStore;
import io.riskstream.risk.store.RiskStateStore.LastSeen;
import java.util.Optional;

/** Same card used in two different countries within a short time. */
public class ImpossibleTravelRule implements Rule {

    private final RiskStateStore store;

    public ImpossibleTravelRule(RiskStateStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "IMPOSSIBLE_TRAVEL";
    }

    @Override
    public Optional<RuleHit> evaluate(Transaction txn, RuleConfig cfg) {
        LastSeen previous = store.swapLastSeen(txn.cardId(), txn.country(), txn.timestamp());
        if (previous == null || previous.country().equals(txn.country())) {
            return Optional.empty();
        }
        long gapSec = Math.abs(txn.timestamp() - previous.timestamp()) / 1000;
        if (gapSec <= cfg.travelWindowSec()) {
            return Optional.of(new RuleHit(name(), cfg.travelPoints(),
                    "Impossible travel: " + previous.country() + " -> " + txn.country()
                            + " within " + gapSec + "s"));
        }
        return Optional.empty();
    }
}
