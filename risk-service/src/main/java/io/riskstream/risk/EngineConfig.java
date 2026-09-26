package io.riskstream.risk;

import io.riskstream.risk.engine.AmountSpikeRule;
import io.riskstream.risk.engine.HighAmountRule;
import io.riskstream.risk.engine.ImpossibleTravelRule;
import io.riskstream.risk.engine.RiskEngine;
import io.riskstream.risk.engine.RuleConfigService;
import io.riskstream.risk.engine.VelocityRule;
import io.riskstream.risk.store.RiskStateStore;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free engine classes into Spring. */
@Configuration
public class EngineConfig {

    @Bean
    RiskEngine riskEngine(RiskStateStore store) {
        return new RiskEngine(List.of(
                new VelocityRule(store),
                new AmountSpikeRule(store),
                new ImpossibleTravelRule(store),
                new HighAmountRule()));
    }

    @Bean
    RuleConfigService ruleConfigService(RiskStateStore store) {
        return new RuleConfigService(store);
    }
}
