package io.riskstream.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.riskstream.common.Alert;
import io.riskstream.common.Alert.Severity;
import io.riskstream.common.Transaction;
import io.riskstream.risk.engine.AmountSpikeRule;
import io.riskstream.risk.engine.HighAmountRule;
import io.riskstream.risk.engine.ImpossibleTravelRule;
import io.riskstream.risk.engine.RiskEngine;
import io.riskstream.risk.engine.RuleConfig;
import io.riskstream.risk.engine.RuleConfigService;
import io.riskstream.risk.engine.VelocityRule;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiskEngineTest {

    private static final long T0 = 1_700_000_000_000L;

    private InMemoryRiskStateStore store;
    private RiskEngine engine;
    private final RuleConfig cfg = RuleConfig.defaults();

    @BeforeEach
    void setUp() {
        store = new InMemoryRiskStateStore();
        engine = new RiskEngine(List.of(
                new VelocityRule(store),
                new AmountSpikeRule(store),
                new ImpossibleTravelRule(store),
                new HighAmountRule()));
    }

    private static Transaction txn(String card, double amount, String country, long ts) {
        return new Transaction(UUID.randomUUID().toString(), card, "shop",
                BigDecimal.valueOf(amount), "EUR", country, ts);
    }

    @Test
    void normalTrafficRaisesNoAlerts() {
        for (int i = 0; i < 10; i++) {
            Optional<Alert> alert = engine.evaluate(txn("c1", 30, "DE", T0 + i * 120_000L), cfg);
            assertTrue(alert.isEmpty(), "transaction " + i + " should not alert");
        }
    }

    @Test
    void velocityBurstTripsTheRule() {
        Optional<Alert> last = Optional.empty();
        for (int i = 0; i < 8; i++) {
            last = engine.evaluate(txn("c2", 20, "DE", T0 + i * 200L), cfg);
        }
        assertTrue(last.isPresent());
        assertEquals(Severity.MEDIUM, last.get().severity());
        assertTrue(last.get().reasons().get(0).startsWith("Velocity"));
    }

    @Test
    void velocityWindowSlides() {
        // 6 transactions are allowed per 60 s. Space them 20 s apart: never more than 3 in a window.
        for (int i = 0; i < 20; i++) {
            assertTrue(engine.evaluate(txn("c3", 20, "DE", T0 + i * 20_000L), cfg).isEmpty());
        }
    }

    @Test
    void amountSpikeNeedsHistoryThenFires() {
        // Not enough history yet: a big first purchase does not trip the spike rule.
        assertTrue(engine.evaluate(txn("c4", 400, "DE", T0), cfg).isEmpty());
        for (int i = 1; i <= 4; i++) {
            engine.evaluate(txn("c5", 20, "DE", T0 + i * 100_000L), cfg);
        }
        Optional<Alert> spike = engine.evaluate(txn("c5", 400, "DE", T0 + 600_000L), cfg);
        assertTrue(spike.isPresent());
        assertTrue(spike.get().reasons().get(0).startsWith("Amount spike"));
    }

    @Test
    void impossibleTravelFires() {
        engine.evaluate(txn("c6", 25, "DE", T0), cfg);
        Optional<Alert> alert = engine.evaluate(txn("c6", 25, "IN", T0 + 2_000L), cfg);
        assertTrue(alert.isPresent());
        assertEquals(50, alert.get().score());
        assertEquals(Severity.MEDIUM, alert.get().severity());
    }

    @Test
    void sameCountryOrLongGapIsNotTravel() {
        engine.evaluate(txn("c7", 25, "DE", T0), cfg);
        assertTrue(engine.evaluate(txn("c7", 25, "DE", T0 + 2_000L), cfg).isEmpty());
        assertTrue(engine.evaluate(txn("c7", 25, "IN", T0 + 3_600_000L), cfg).isEmpty());
    }

    @Test
    void combinedRulesEscalateSeverity() {
        // Build history, then a huge purchase abroad right after: spike + high amount + travel.
        for (int i = 0; i < 4; i++) {
            engine.evaluate(txn("c8", 20, "DE", T0 + i * 100_000L), cfg);
        }
        Optional<Alert> alert = engine.evaluate(txn("c8", 9000, "US", T0 + 400_000L), cfg);
        assertTrue(alert.isPresent());
        assertEquals(100, alert.get().score()); // 45 + 30 + 50 = 125, capped at 100
        assertEquals(Severity.CRITICAL, alert.get().severity());
        assertEquals(3, alert.get().reasons().size());
    }

    @Test
    void idempotencyMarkIsFirstWriterWins() {
        assertTrue(store.markSeen("t1"));
        assertFalse(store.markSeen("t1"));
        store.unmarkSeen("t1");
        assertTrue(store.markSeen("t1"));
    }

    @Test
    void configFallsBackToDefaultsAndValidates() {
        RuleConfigService service = new RuleConfigService(store);
        assertEquals(RuleConfig.defaults(), service.current());

        RuleConfig tuned = new RuleConfig(3, 30, 40, 5.0, 2, 35, 300, 50, 1000.0, 30, 40);
        assertEquals(tuned, service.update(tuned));
        assertEquals(tuned, RuleConfig.fromMap(store.readConfig()));

        RuleConfig invalid = new RuleConfig(0, 60, 40, 8.0, 3, 35, 600, 50, 5000.0, 30, 40);
        assertThrows(IllegalArgumentException.class, () -> service.update(invalid));

        assertEquals(RuleConfig.defaults(), service.reset());
    }

    @Test
    void corruptStoredConfigFallsBackToDefaults() {
        store.writeConfig(Map.of("velocityMaxTxns", "not-a-number"));
        assertEquals(RuleConfig.defaults(), RuleConfig.fromMap(store.readConfig()));
    }
}
