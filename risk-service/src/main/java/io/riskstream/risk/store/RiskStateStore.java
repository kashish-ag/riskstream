package io.riskstream.risk.store;

import java.util.Map;

/**
 * Everything the risk engine needs to remember between transactions. The production
 * implementation is Redis ({@link RedisRiskStateStore}); tests use an in-memory one, which
 * keeps the rule logic unit-testable without any infrastructure.
 */
public interface RiskStateStore {

    /** Idempotency guard. Returns true only the first time a transaction id is seen. */
    boolean markSeen(String txnId);

    /** Releases the idempotency mark so a failed transaction can be retried. */
    void unmarkSeen(String txnId);

    /**
     * Sliding-window counter: records the transaction for the card and returns how many
     * transactions (including this one) fall inside the last {@code windowMs} milliseconds.
     */
    long recordAndCount(String cardId, String txnId, long tsMillis, long windowMs);

    /** Running spend statistics for a card (count 0 when the card has no history). */
    CardStats cardStats(String cardId);

    /** Adds an amount to the card's running statistics. */
    void recordAmount(String cardId, double amount);

    /** Stores the card's latest country/time and returns the previous one, or null. */
    LastSeen swapLastSeen(String cardId, String country, long tsMillis);

    Map<String, String> readConfig();

    void writeConfig(Map<String, String> values);

    void clearConfig();

    record CardStats(long count, double sum) {
        public double average() {
            return count == 0 ? 0 : sum / count;
        }
    }

    record LastSeen(String country, long timestamp) {
    }
}
