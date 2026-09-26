package io.riskstream.producer;

import io.riskstream.common.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares topics so a fresh broker gets 3 partitions instead of the auto-create default of 1.
 * The risk-service declares the same topics; creation is idempotent.
 */
@Configuration
public class TopicConfig {

    @Bean
    NewTopic transactionsTopic() {
        return TopicBuilder.name(Topics.TRANSACTIONS).partitions(3).replicas(1).build();
    }
}
