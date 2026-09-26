#!/usr/bin/env bash
# Steps the producer through increasing transaction rates and prints risk-service stats after each.
# Usage: scripts/bench.sh            (stack must be running: docker compose up --build)
set -euo pipefail

PRODUCER=${PRODUCER_URL:-http://localhost:8081}
RISK=${RISK_URL:-http://localhost:8082}
SECONDS_PER_STEP=${SECONDS_PER_STEP:-20}

processed() { curl -s "$RISK/api/stats" | sed -n 's/.*"processed":\([0-9]*\).*/\1/p'; }

echo "target_tps  achieved_tps  scoring_p50_ms  scoring_p99_ms"
for tps in 100 500 1000 2000; do
  curl -s -X POST "$PRODUCER/api/producer/rate?tps=$tps" >/dev/null
  sleep 5                                   # let the rate settle
  start=$(processed); sleep "$SECONDS_PER_STEP"; end=$(processed)
  achieved=$(( (end - start) / SECONDS_PER_STEP ))
  stats=$(curl -s "$RISK/api/stats")
  p50=$(echo "$stats" | sed -n 's/.*"scoringP50Ms":\([0-9.]*\).*/\1/p')
  p99=$(echo "$stats" | sed -n 's/.*"scoringP99Ms":\([0-9.]*\).*/\1/p')
  printf "%-11s %-13s %-15s %s\n" "$tps" "$achieved" "$p50" "$p99"
done
curl -s -X POST "$PRODUCER/api/producer/rate?tps=20" >/dev/null   # back to normal
