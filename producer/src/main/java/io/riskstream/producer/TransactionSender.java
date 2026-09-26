package io.riskstream.producer;

import io.riskstream.common.Topics;
import io.riskstream.common.Transaction;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes transactions keyed by card id. Keying by card guarantees that all events for one
 * card land on the same partition, so the risk-service sees them in order.
 */
@Component
public class TransactionSender {

    private static final Logger log = LoggerFactory.getLogger(TransactionSender.class);

    private final KafkaTemplate<String, Transaction> kafka;
    private final AtomicLong sent = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public TransactionSender(KafkaTemplate<String, Transaction> kafka) {
        this.kafka = kafka;
    }

    public void send(Transaction txn) {
        kafka.send(Topics.TRANSACTIONS, txn.cardId(), txn).whenComplete((result, error) -> {
            if (error != null) {
                failed.incrementAndGet();
                log.warn("Failed to publish txn {}: {}", txn.id(), error.toString());
            } else {
                sent.incrementAndGet();
            }
        });
    }

    public long sent() {
        return sent.get();
    }

    public long failed() {
        return failed.get();
    }
}
