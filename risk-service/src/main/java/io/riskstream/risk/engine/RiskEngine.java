package io.riskstream.risk.engine;

import io.riskstream.common.Alert;
import io.riskstream.common.Alert.Severity;
import io.riskstream.common.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs every rule against a transaction and sums the points. If the total reaches the alert
 * threshold an {@link Alert} is produced. All rules always run (no short-circuit) so that card
 * state such as the velocity window stays accurate even for transactions that are not flagged.
 */
public class RiskEngine {

    private final List<Rule> rules;

    public RiskEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Optional<Alert> evaluate(Transaction txn, RuleConfig cfg) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        for (Rule rule : rules) {
            Optional<RuleHit> hit = rule.evaluate(txn, cfg);
            if (hit.isPresent()) {
                score += hit.get().points();
                reasons.add(hit.get().reason());
            }
        }
        score = Math.min(score, 100);
        if (score < cfg.alertThreshold()) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        return Optional.of(new Alert(
                UUID.randomUUID().toString(), txn.id(), txn.cardId(), txn.merchant(),
                txn.amount(), txn.currency(), txn.country(), score, severityFor(score), reasons,
                txn.timestamp(), now, Math.max(0, now - txn.timestamp())));
    }

    static Severity severityFor(int score) {
        if (score >= 90) {
            return Severity.CRITICAL;
        }
        if (score >= 60) {
            return Severity.HIGH;
        }
        return Severity.MEDIUM;
    }
}
