package io.riskstream.risk;

import io.riskstream.risk.store.RiskStateStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Infrastructure-free {@link RiskStateStore} used to unit-test rules and the engine. */
public class InMemoryRiskStateStore implements RiskStateStore {

    private final Set<String> seen = new HashSet<>();
    private final Map<String, Map<String, Long>> windows = new HashMap<>();
    private final Map<String, CardStats> stats = new HashMap<>();
    private final Map<String, LastSeen> last = new HashMap<>();
    private final Map<String, String> config = new HashMap<>();

    @Override
    public boolean markSeen(String txnId) {
        return seen.add(txnId);
    }

    @Override
    public void unmarkSeen(String txnId) {
        seen.remove(txnId);
    }

    @Override
    public long recordAndCount(String cardId, String txnId, long tsMillis, long windowMs) {
        Map<String, Long> window = windows.computeIfAbsent(cardId, k -> new HashMap<>());
        window.values().removeIf(ts -> ts <= tsMillis - windowMs);
        window.put(txnId, tsMillis);
        return window.size();
    }

    @Override
    public CardStats cardStats(String cardId) {
        return stats.getOrDefault(cardId, new CardStats(0, 0));
    }

    @Override
    public void recordAmount(String cardId, double amount) {
        CardStats s = cardStats(cardId);
        stats.put(cardId, new CardStats(s.count() + 1, s.sum() + amount));
    }

    @Override
    public LastSeen swapLastSeen(String cardId, String country, long tsMillis) {
        return last.put(cardId, new LastSeen(country, tsMillis));
    }

    @Override
    public Map<String, String> readConfig() {
        return new HashMap<>(config);
    }

    @Override
    public void writeConfig(Map<String, String> values) {
        config.putAll(values);
    }

    @Override
    public void clearConfig() {
        config.clear();
    }
}
