# Walkthrough — explaining riskstream in an interview

A cheat-sheet to explain every part of this project confidently. Read the code alongside it.

## 60-second pitch

"I built a real-time transaction risk engine. A producer streams synthetic card transactions into Kafka, keyed by card ID. A Spring Boot consumer scores each one against fraud rules — velocity, amount spikes, impossible travel — using Redis for the per-card state, and publishes alerts to a second topic. A React dashboard receives them live over SSE, and lets you tune the rule thresholds at runtime because they're stored in Redis. It's idempotent, has retry plus a dead-letter topic, and runs with one `docker compose up`."

## Follow the life of one transaction

1. `TransactionGenerator` creates a fake transaction; `TransactionSender` publishes it to `transactions` with `cardId` as the **key**.
2. Kafka hashes the key to one of 3 partitions, so the same card always lands on the same partition.
3. `TransactionConsumer` (3 consumer threads, one per partition) receives it.
4. **Idempotency:** `markSeen` does `SET seen:{id} NX` with a 1 h TTL. If the key already existed, the message is a redelivery and is skipped.
5. `RuleConfigService.current()` returns thresholds (Redis hash, 1 s in-process cache).
6. `RiskEngine` runs every rule. Velocity uses a Lua script on a sorted set; amount spike and travel use per-card hashes.
7. Points are summed (capped at 100). At or above the threshold an `Alert` is created and published to `alerts`.
8. `AlertStream` (a listener on `alerts`) pushes it to browsers over SSE; the React app renders it.

## Questions you should be ready for

**Why key by card ID?** Ordering. Kafka only guarantees order within a partition. Rules like velocity and impossible travel depend on a card's events arriving in order. Keying by card gives per-card ordering *and* parallelism across cards.

**What if a consumer crashes mid-processing?** The offset isn't committed until the batch finishes, so Kafka redelivers. That's at-least-once. The idempotency key stops double-scoring. If an exception is thrown we release the key so the retry can proceed. There is a small window (crash after `markSeen`, before scoring finishes, with no exception) where a transaction could be skipped; a production system would store the result atomically with the mark, or use Kafka transactions. Say this out loud — it shows you understand the trade-off.

**Why a Lua script for the window?** Trim + add + count must be atomic. Without it, two operations from concurrent consumers could interleave. Lua runs atomically inside Redis and saves round trips.

**Why sorted set for a sliding window?** Score = event time, so removing everything older than `now - window` is a single range delete and `ZCARD` is O(1).

**Why is the rule engine independent of Spring?** So it is unit-testable in milliseconds with an in-memory store, and the infrastructure code (Redis) is tested separately. It also shows separation of concerns.

**How do retries and the DLT work?** `DefaultErrorHandler` with a fixed backoff (3 retries). After that, a recoverer publishes the record plus the error to `transactions.DLT`. `ErrorHandlingDeserializer` converts undeserializable messages into normal errors so they take the same path instead of blocking the partition forever.

**Why SSE instead of WebSockets?** Data only flows server to client; SSE is simpler, auto-reconnects, and works through plain HTTP proxies (nginx needs `proxy_buffering off`).

**Why does every instance use a different consumer group for alerts?** Each instance holds its own SSE connections, so each needs every alert. A shared group would split alerts between instances.

**How would you scale it?** More partitions and more instances of the risk service (consumers in one group split partitions); Redis Cluster with keys hashed per card; Kafka replication factor 3; move to MSK/ElastiCache.

**What are the weaknesses?** Heuristic rules only; single broker/Redis; `double` is used inside rule math (amounts are `BigDecimal` on the wire). Naming the weaknesses yourself is a strength.

## Files worth knowing by heart

- `risk-service/.../kafka/TransactionConsumer.java` — the hot path
- `risk-service/.../store/RedisRiskStateStore.java` — Redis key layout and the Lua script
- `risk-service/.../engine/RiskEngine.java` and the four rule classes
- `risk-service/.../kafka/KafkaConfig.java` — retries and dead-letter handling
- `docker-compose.yml` — KRaft Kafka setup (two listeners: one for containers, one for the host)
