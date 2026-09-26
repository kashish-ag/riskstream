package io.riskstream.common;

/** Kafka topic names shared by producer and risk-service. */
public final class Topics {
    public static final String TRANSACTIONS = "transactions";
    public static final String TRANSACTIONS_DLT = "transactions.DLT";
    public static final String ALERTS = "alerts";

    private Topics() {
    }
}
