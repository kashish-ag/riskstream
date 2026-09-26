package io.riskstream.risk.kafka;

import io.riskstream.common.Topics;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    NewTopic transactionsTopic() {
        return TopicBuilder.name(Topics.TRANSACTIONS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic alertsTopic() {
        return TopicBuilder.name(Topics.ALERTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic deadLetterTopic() {
        return TopicBuilder.name(Topics.TRANSACTIONS_DLT).partitions(1).replicas(1).build();
    }

    /**
     * Failed records are retried 3 times (500 ms apart). If they still fail, or if they cannot
     * even be deserialized (poison pill), they are parked on the dead-letter topic with the
     * error attached, so one bad message never blocks a partition.
     */
    @Bean
    CommonErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        return new DefaultErrorHandler((ConsumerRecord<?, ?> record, Exception ex) -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            Map<String, Object> dead = new HashMap<>();
            dead.put("error", String.valueOf(cause));
            dead.put("topic", record.topic());
            dead.put("partition", record.partition());
            dead.put("offset", record.offset());
            dead.put("key", record.key());
            dead.put("value", record.value());
            log.error("Sending record {}-{}@{} to dead-letter topic: {}",
                    record.topic(), record.partition(), record.offset(), cause.toString());
            String key = record.key() == null ? null : String.valueOf(record.key());
            template.send(Topics.TRANSACTIONS_DLT, key, dead);
        }, new FixedBackOff(500L, 3L));
    }
}
