package io.riskstream.producer;

import io.riskstream.common.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates synthetic (fully fake) card transactions. Every card has a deterministic home
 * country and typical spend, so "normal" traffic is well behaved and fraud patterns stand out.
 */
@Component
public class TransactionGenerator {

    static final String[] COUNTRIES = {"IN", "DE", "NL", "IE", "GB", "FR", "ES", "US"};
    private static final String[] MERCHANTS = {
            "grocery-mart", "coffee-corner", "ride-share", "online-books", "fuel-station",
            "electronics-hub", "pharmacy-plus", "streaming-sub", "food-delivery", "hotel-stay"};

    private final int cardPool;

    public TransactionGenerator(@Value("${riskstream.producer.card-pool:5000}") int cardPool) {
        this.cardPool = cardPool;
    }

    /** One ordinary purchase from a random card, in its home country, near its typical amount. */
    public Transaction normal() {
        int idx = ThreadLocalRandom.current().nextInt(cardPool);
        double factor = 0.5 + ThreadLocalRandom.current().nextDouble();
        return build(idx, homeCountry(idx), baseAmount(idx) * factor, System.currentTimeMillis());
    }

    /** Many rapid purchases on one card: should trip the velocity rule. */
    public List<Transaction> velocityBurst() {
        int idx = randomCard();
        List<Transaction> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 12; i++) {
            out.add(build(idx, homeCountry(idx), baseAmount(idx) * 0.6, now + i * 200L));
        }
        return out;
    }

    /** Home-country purchase followed seconds later by one in another country: impossible travel. */
    public List<Transaction> impossibleTravel() {
        int idx = randomCard();
        long now = System.currentTimeMillis();
        String home = homeCountry(idx);
        String far = COUNTRIES[(idx + 3) % COUNTRIES.length];
        return List.of(
                build(idx, home, baseAmount(idx), now),
                build(idx, far, baseAmount(idx), now + 2_000L));
    }

    /** A few normal purchases to build history, then one purchase ~30x the typical amount. */
    public List<Transaction> amountSpike() {
        int idx = randomCard();
        List<Transaction> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            out.add(build(idx, homeCountry(idx), baseAmount(idx), now + i * 100L));
        }
        out.add(build(idx, homeCountry(idx), baseAmount(idx) * 30, now + 1_000L));
        return out;
    }

    private Transaction build(int cardIdx, String country, double amount, long ts) {
        String merchant = MERCHANTS[ThreadLocalRandom.current().nextInt(MERCHANTS.length)];
        BigDecimal value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        return new Transaction(UUID.randomUUID().toString(), cardId(cardIdx), merchant, value,
                "EUR", country, ts);
    }

    private int randomCard() {
        return ThreadLocalRandom.current().nextInt(cardPool);
    }

    static String cardId(int idx) {
        return String.format("card-%05d", idx);
    }

    static String homeCountry(int idx) {
        return COUNTRIES[idx % COUNTRIES.length];
    }

    static double baseAmount(int idx) {
        return 10 + (idx % 50) * 3;
    }
}
