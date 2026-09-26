package io.riskstream.risk.kafka;

import io.riskstream.common.Alert;
import io.riskstream.common.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes alerts to Kafka so any downstream consumer (case management, notifications, ...) can react. */
@Component
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public AlertPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publish(Alert alert) {
        kafka.send(Topics.ALERTS, alert.cardId(), alert).whenComplete((result, error) -> {
            if (error != null) {
                log.warn("Failed to publish alert {}: {}", alert.alertId(), error.toString());
            }
        });
    }
}
