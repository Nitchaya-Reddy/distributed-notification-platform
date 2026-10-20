# Resume entry

## Distributed Notification Platform | Java, Spring Boot, Kafka, Redis, PostgreSQL, Docker, Kubernetes

- Engineered an event-driven notification platform using **Java 21, Spring Boot, Apache Kafka, Redis, and PostgreSQL**, decoupling request ingestion from asynchronous EMAIL, SMS, PUSH, and IN_APP delivery through partitioned consumer groups.
- Implemented a **transactional outbox** to prevent database/Kafka dual-write loss, plus durable per-channel idempotency for safe at-least-once Kafka redelivery without claiming unrealistic global exactly-once semantics.
- Designed independent channel fan-out with **exponential retries, dead-letter routing, Redis/Lua rate limiting, scheduled delivery, and idempotency keys**, isolating provider failures while supporting horizontal worker scaling.
- Parallelized Kafka processing across **12 delivery partitions / 6 consumers, 6 request partitions / 3 dispatchers, and 3 status consumers**; validate final performance claims with the corrected benchmark before adding latency or throughput numbers.
- Containerized API and worker services with **Docker**, added health/Prometheus endpoints, GitHub Actions CI, Flyway migrations, OpenAPI documentation, and Kubernetes deployment manifests.

## Metrics rule

Use only results produced by the corrected `scripts/load-test.py`. Record the exact request count, concurrency, environment, commit SHA, provider mode, acceptance rate, p50/p95 latency, and terminal delivery rate. Do not convert old benchmark wall time into a throughput claim because the legacy poller could distort total wall time.
