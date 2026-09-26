package io.riskstream.risk.engine;

import io.riskstream.common.Transaction;
import java.util.Optional;

/**
 * A single fraud-detection rule. Rules may update the card state they depend on (for example
 * appending to the velocity window) as part of evaluation.
 */
public interface Rule {

    String name();

    Optional<RuleHit> evaluate(Transaction txn, RuleConfig config);
}
