# Interview guide

## 30-second explanation

I built an event-driven notification platform where the API persists a notification and a transactional outbox row in one PostgreSQL transaction. An outbox publisher sends events to Kafka, a dispatcher fans each notification out into independent channel commands, and horizontally scalable workers process those commands with Redis rate limiting, durable idempotency, retry topics, and a dead-letter path. I chose at-least-once semantics because external providers make end-to-end exactly-once unrealistic, and I handle Kafka redelivery with a unique `(notification, channel)` delivery ledger.

## Questions to be ready for

1. **Why not call email/SMS providers directly from the REST request?**  
   It couples user latency and availability to slow external providers and cannot absorb traffic spikes well.

2. **Why Kafka?**  
   Durable buffering, replay, partitioned consumer-group scaling, and natural decoupling between ingestion and delivery.

3. **Why transactional outbox?**  
   To remove the database/Kafka dual-write gap.

4. **Why not exactly once?**  
   Kafka transactions do not make an external email/SMS side effect exactly once. Provider idempotency keys plus a durable delivery ledger are the practical approach.

5. **How does it scale?**  
   Add stateless API replicas and worker replicas. Delivery throughput scales by Kafka partition count until the provider, Redis, or database becomes the bottleneck.

6. **Why Redis?**  
   Shared low-latency rate-limit counters across many workers. A Lua script makes increment + TTL creation atomic.

7. **What would you change at 100M notifications/day?**  
   CDC-based outbox publishing, separate databases/service ownership, partitioned archival storage, per-provider topics/pools, adaptive token-bucket limits, autoscaling from Kafka lag, and provider-specific circuit breakers.
