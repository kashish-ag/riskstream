package io.riskstream.risk.store;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis-backed state. Key layout:
 * <pre>
 *   seen:{txnId}      string, TTL 1h        idempotency marker
 *   vel:{cardId}      sorted set            sliding window (score = event time ms, member = txnId)
 *   stats:{cardId}    hash count,sum        running spend statistics
 *   last:{cardId}     hash country,ts       last known location
 *   config:rules      hash                  live rule thresholds
 * </pre>
 */
@Component
public class RedisRiskStateStore implements RiskStateStore {

    static final String CONFIG_KEY = "config:rules";
    private static final Duration SEEN_TTL = Duration.ofHours(1);
    private static final Duration CARD_TTL = Duration.ofDays(30);

    /**
     * Atomic sliding window. Trimming, insert and count happen in one Redis round trip, so
     * concurrent consumers can never observe a half-updated window.
     */
    private static final String WINDOW_LUA = """
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)
            redis.call('ZADD', KEYS[1], now, ARGV[3])
            redis.call('PEXPIRE', KEYS[1], window * 2)
            return redis.call('ZCARD', KEYS[1])
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> windowScript = new DefaultRedisScript<>(WINDOW_LUA, Long.class);

    public RedisRiskStateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean markSeen(String txnId) {
        Boolean first = redis.opsForValue().setIfAbsent("seen:" + txnId, "1", SEEN_TTL);
        return Boolean.TRUE.equals(first);
    }

    @Override
    public void unmarkSeen(String txnId) {
        redis.delete("seen:" + txnId);
    }

    @Override
    public long recordAndCount(String cardId, String txnId, long tsMillis, long windowMs) {
        Long count = redis.execute(windowScript, List.of("vel:" + cardId),
                String.valueOf(tsMillis), String.valueOf(windowMs), txnId);
        return count == null ? 0 : count;
    }

    @Override
    public CardStats cardStats(String cardId) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        Map<String, String> entries = hash.entries("stats:" + cardId);
        if (entries.isEmpty()) {
            return new CardStats(0, 0);
        }
        return new CardStats(Long.parseLong(entries.getOrDefault("count", "0")),
                Double.parseDouble(entries.getOrDefault("sum", "0")));
    }

    @Override
    public void recordAmount(String cardId, double amount) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        String key = "stats:" + cardId;
        hash.increment(key, "count", 1L);
        hash.increment(key, "sum", amount);
        redis.expire(key, CARD_TTL);
    }

    @Override
    public LastSeen swapLastSeen(String cardId, String country, long tsMillis) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        String key = "last:" + cardId;
        Map<String, String> previous = hash.entries(key);
        Map<String, String> next = new HashMap<>();
        next.put("country", country);
        next.put("ts", String.valueOf(tsMillis));
        hash.putAll(key, next);
        redis.expire(key, CARD_TTL);
        if (previous.isEmpty()) {
            return null;
        }
        return new LastSeen(previous.get("country"), Long.parseLong(previous.get("ts")));
    }

    @Override
    public Map<String, String> readConfig() {
        HashOperations<String, String, String> hash = redis.opsForHash();
        return hash.entries(CONFIG_KEY);
    }

    @Override
    public void writeConfig(Map<String, String> values) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        hash.putAll(CONFIG_KEY, values);
    }

    @Override
    public void clearConfig() {
        redis.delete(CONFIG_KEY);
    }
}
