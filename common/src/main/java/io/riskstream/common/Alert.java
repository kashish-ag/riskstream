package io.riskstream.common;

import java.math.BigDecimal;
import java.util.List;

/** A risk alert raised for a transaction whose combined rule score crossed the threshold. */
public record Alert(
        String alertId,
        String txnId,
        String cardId,
        String merchant,
        BigDecimal amount,
        String currency,
        String country,
        int score,
        Severity severity,
        List<String> reasons,
        long txnTimestamp,
        long raisedAt,
        long endToEndLatencyMs) {

    public enum Severity { MEDIUM, HIGH, CRITICAL }
}
