package io.riskstream.risk.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.riskstream.common.Alert;
import io.riskstream.common.Topics;
import io.riskstream.common.Transaction;
import io.riskstream.risk.engine.RiskEngine;
import io.riskstream.risk.engine.RuleConfig;
import io.riskstream.risk.engine.RuleConfigService;
import io.riskstream.risk.store.RiskStateStore;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes transactions and scores them. Delivery from Kafka is at-least-once, so a redelivered
 * transaction must not be scored twice: an idempotency marker in Redis makes processing
 * effectively-once for the state we keep. If scoring throws, the marker is released so the
 * retry (and eventually the dead-letter topic) sees a clean slate.
 */
@Component
public class TransactionConsumer {

    private final RiskStateStore store;
    private final RiskEngine engine;
    private final RuleConfigService configService;
    private final AlertPublisher alertPublisher;
    private final Counter processed;
    private final Counter duplicates;
    private final Counter alerts;
    private final Timer scoring;

    public TransactionConsumer(RiskStateStore store, RiskEngine engine, RuleConfigService configService,
                               AlertPublisher alertPublisher, MeterRegistry registry) {
        this.store = store;
        this.engine = engine;
        this.configService = configService;
        this.alertPublisher = alertPublisher;
        this.processed = registry.counter("riskstream.transactions.processed");
        this.duplicates = registry.counter("riskstream.transactions.duplicates");
        this.alerts = registry.counter("riskstream.alerts.raised");
        this.scoring = Timer.builder("riskstream.scoring.time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @KafkaListener(topics = Topics.TRANSACTIONS, groupId = "risk-service")
    public void onTransaction(Transaction txn) {
        if (!store.markSeen(txn.id())) {
            duplicates.increment();
            return;
        }
        try {
            RuleConfig config = configService.current();
            long started = System.nanoTime();
            Optional<Alert> alert = engine.evaluate(txn, config);
            scoring.record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
            processed.increment();
            if (alert.isPresent()) {
                alerts.increment();
                alertPublisher.publish(alert.get());
            }
        } catch (RuntimeException e) {
            store.unmarkSeen(txn.id());
            throw e;
        }
    }
}
