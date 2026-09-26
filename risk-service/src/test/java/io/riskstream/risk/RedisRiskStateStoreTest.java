package io.riskstream.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.riskstream.risk.engine.RuleConfig;
import io.riskstream.risk.store.RedisRiskStateStore;
import io.riskstream.risk.store.RiskStateStore.CardStats;
import io.riskstream.risk.store.RiskStateStore.LastSeen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the Redis-backed store against a real Redis (Testcontainers). Skipped automatically
 * when Docker is not available, so `mvn test` still works on a machine without it.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisRiskStateStoreTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static LettuceConnectionFactory factory;
    static RedisRiskStateStore store;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        store = new RedisRiskStateStore(template);
    }

    @AfterAll
    static void disconnect() {
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    void slidingWindowCountsAndTrims() {
        assertEquals(1, store.recordAndCount("win-card", "a", 1_000, 60_000));
        assertEquals(2, store.recordAndCount("win-card", "b", 2_000, 60_000));
        assertEquals(2, store.recordAndCount("win-card", "b", 2_000, 60_000)); // same id: idempotent
        assertEquals(1, store.recordAndCount("win-card", "c", 200_000, 60_000)); // old entries trimmed
    }

    @Test
    void idempotencyMarkIsFirstWriterWins() {
        assertTrue(store.markSeen("txn-1"));
        assertFalse(store.markSeen("txn-1"));
        store.unmarkSeen("txn-1");
        assertTrue(store.markSeen("txn-1"));
    }

    @Test
    void runningStatsAccumulate() {
        assertEquals(0, store.cardStats("stats-card").count());
        store.recordAmount("stats-card", 10.0);
        store.recordAmount("stats-card", 30.0);
        CardStats stats = store.cardStats("stats-card");
        assertEquals(2, stats.count());
        assertEquals(20.0, stats.average());
    }

    @Test
    void lastSeenReturnsPreviousLocation() {
        assertNull(store.swapLastSeen("geo-card", "DE", 1_000));
        LastSeen previous = store.swapLastSeen("geo-card", "IN", 2_000);
        assertEquals("DE", previous.country());
        assertEquals(1_000, previous.timestamp());
    }

    @Test
    void configRoundTripsThroughRedis() {
        RuleConfig tuned = new RuleConfig(3, 30, 40, 5.0, 2, 45, 300, 50, 1000.0, 30, 40);
        store.writeConfig(tuned.toMap());
        assertEquals(tuned, RuleConfig.fromMap(store.readConfig()));
        store.clearConfig();
        assertEquals(RuleConfig.defaults(), RuleConfig.fromMap(store.readConfig()));
    }
}
