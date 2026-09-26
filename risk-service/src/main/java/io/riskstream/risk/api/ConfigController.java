package io.riskstream.risk.api;

import io.riskstream.risk.engine.RuleConfig;
import io.riskstream.risk.engine.RuleConfigService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Live rule tuning: thresholds are stored in Redis and picked up without a redeploy. */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final RuleConfigService service;

    public ConfigController(RuleConfigService service) {
        this.service = service;
    }

    @GetMapping
    public RuleConfig get() {
        return service.current();
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody RuleConfig config) {
        try {
            return ResponseEntity.ok(service.update(config));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public RuleConfig reset() {
        return service.reset();
    }
}
