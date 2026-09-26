package io.riskstream.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Emits steady traffic. Runs every 100 ms and sends tps/10 transactions per tick. */
@Component
public class GeneratorScheduler {

    private final TransactionGenerator generator;
    private final TransactionSender sender;
    private volatile int tps;
    private double carry;

    public GeneratorScheduler(TransactionGenerator generator, TransactionSender sender,
                              @Value("${riskstream.producer.tps:20}") int tps) {
        this.generator = generator;
        this.sender = sender;
        this.tps = tps;
    }

    @Scheduled(fixedRate = 100)
    void tick() {
        carry += tps / 10.0;
        int n = (int) carry;
        carry -= n;
        for (int i = 0; i < n; i++) {
            sender.send(generator.normal());
        }
    }

    public int tps() {
        return tps;
    }

    public void setTps(int tps) {
        this.tps = tps;
    }
}
