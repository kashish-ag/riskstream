package io.riskstream.risk.api;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight stats for the dashboard tiles. Full metrics are on /actuator/prometheus. */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final MeterRegistry registry;
    private final long startedAt = System.currentTimeMillis();

    public StatsController(MeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public Map<String, Object> stats() {
        Timer timer = registry.timer("riskstream.scoring.time");
        HistogramSnapshot snapshot = timer.takeSnapshot();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("processed", (long) registry.counter("riskstream.transactions.processed").count());
        out.put("duplicates", (long) registry.counter("riskstream.transactions.duplicates").count());
        out.put("alerts", (long) registry.counter("riskstream.alerts.raised").count());
        out.put("scoringMeanMs", round(timer.mean(TimeUnit.MICROSECONDS) / 1000.0));
        for (ValueAtPercentile p : snapshot.percentileValues()) {
            out.put("scoringP" + (int) Math.round(p.percentile() * 100) + "Ms",
                    round(p.value(TimeUnit.MICROSECONDS) / 1000.0));
        }
        out.put("uptimeSec", (System.currentTimeMillis() - startedAt) / 1000);
        return out;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
