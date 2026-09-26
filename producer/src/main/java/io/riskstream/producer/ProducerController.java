package io.riskstream.producer;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Control plane for the demo: change traffic rate and inject fraud patterns on demand. */
@RestController
@RequestMapping("/api/producer")
public class ProducerController {

    private final GeneratorScheduler scheduler;
    private final TransactionGenerator generator;
    private final TransactionSender sender;

    public ProducerController(GeneratorScheduler scheduler, TransactionGenerator generator,
                              TransactionSender sender) {
        this.scheduler = scheduler;
        this.generator = generator;
        this.sender = sender;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("tps", scheduler.tps(), "sent", sender.sent(), "failed", sender.failed());
    }

    @PostMapping("/rate")
    public ResponseEntity<Map<String, Object>> setRate(@RequestParam int tps) {
        if (tps < 0 || tps > 5000) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "tps must be between 0 and 5000"));
        }
        scheduler.setTps(tps);
        return ResponseEntity.ok(Map.of("tps", tps));
    }

    @PostMapping("/fraud/{pattern}")
    public ResponseEntity<Map<String, Object>> inject(@PathVariable String pattern) {
        List<io.riskstream.common.Transaction> txns = switch (pattern.toLowerCase()) {
            case "velocity" -> generator.velocityBurst();
            case "travel" -> generator.impossibleTravel();
            case "spike" -> generator.amountSpike();
            default -> null;
        };
        if (txns == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "pattern must be one of: velocity, travel, spike"));
        }
        txns.forEach(sender::send);
        return ResponseEntity.ok(Map.of("injected", txns.size(), "pattern", pattern.toLowerCase(),
                "cardId", txns.get(0).cardId()));
    }
}
