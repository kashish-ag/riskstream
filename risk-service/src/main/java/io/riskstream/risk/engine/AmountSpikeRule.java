package io.riskstream.risk.engine;

import io.riskstream.common.Transaction;
import io.riskstream.risk.store.RiskStateStore;
import io.riskstream.risk.store.RiskStateStore.CardStats;
import java.util.Locale;
import java.util.Optional;

/** Amount far above the card's own running average (needs a minimum amount of history). */
public class AmountSpikeRule implements Rule {

    private final RiskStateStore store;

    public AmountSpikeRule(RiskStateStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "AMOUNT_SPIKE";
    }

    @Override
    public Optional<RuleHit> evaluate(Transaction txn, RuleConfig cfg) {
        double amount = txn.amount().doubleValue();
        CardStats stats = store.cardStats(txn.cardId());
        Optional<RuleHit> hit = Optional.empty();

        if (stats.count() >= cfg.spikeMinHistory() && stats.average() > 0
                && amount > cfg.spikeMultiplier() * stats.average()) {
            hit = Optional.of(new RuleHit(name(), cfg.spikePoints(),
                    String.format(Locale.ROOT, "Amount spike: %.2f is %.1fx the card average of %.2f",
                            amount, amount / stats.average(), stats.average())));
        }
        // Update history after comparing, so a transaction is never judged against itself.
        store.recordAmount(txn.cardId(), amount);
        return hit;
    }
}
