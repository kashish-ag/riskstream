package io.riskstream.common;

import java.math.BigDecimal;

/**
 * A card transaction event. Timestamps are epoch millis (event time), which keeps the
 * JSON contract simple for both Java and the React dashboard.
 */
public record Transaction(
        String id,
        String cardId,
        String merchant,
        BigDecimal amount,
        String currency,
        String country,
        long timestamp) {
}
