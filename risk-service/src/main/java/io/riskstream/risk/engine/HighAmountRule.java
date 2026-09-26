package io.riskstream.risk.engine;

import io.riskstream.common.Transaction;
import java.util.Locale;
import java.util.Optional;

/** Any single transaction above an absolute threshold. Stateless. */
public class HighAmountRule implements Rule {

    @Override
    public String name() {
        return "HIGH_AMOUNT";
    }

    @Override
    public Optional<RuleHit> evaluate(Transaction txn, RuleConfig cfg) {
        double amount = txn.amount().doubleValue();
        if (amount >= cfg.highAmountThreshold()) {
            return Optional.of(new RuleHit(name(), cfg.highAmountPoints(),
                    String.format(Locale.ROOT, "High amount: %.2f (threshold %.2f)",
                            amount, cfg.highAmountThreshold())));
        }
        return Optional.empty();
    }
}
