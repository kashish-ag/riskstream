package io.riskstream.risk.engine;

/** One rule firing: which rule, how many points it contributes, and a human-readable reason. */
public record RuleHit(String rule, int points, String reason) {
}
