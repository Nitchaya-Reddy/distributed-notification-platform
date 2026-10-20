# Engineering tradeoffs

| Decision | Why | Cost / alternative |
|---|---|---|
| Kafka over synchronous HTTP | absorbs traffic spikes, decouples API latency from provider latency, consumer-group scaling | more infrastructure and eventual consistency |
| Transactional outbox | prevents lost events across DB/Kafka dual writes | polling adds a small publish delay; CDC/Debezium is a stronger high-scale alternative |
| At-least-once + idempotency | realistic with Kafka and external providers | true exactly-once cannot be guaranteed once an external email/SMS provider is involved |
| Per-channel fan-out | independent retries and scaling | more Kafka records and topics |
| Retry topics + DLT | keeps poison/transient failures away from the main consumer | retry topics can relax ordering guarantees |
| Redis fixed-window limiter | fast, shared across worker replicas, simple atomic Lua operation | burstier than token-bucket/sliding-window algorithms |
| PostgreSQL delivery ledger | durable deduplication and auditability | write amplification; very high volume systems may use a separate write-optimized store |
| Local shared Postgres instance | one-command developer experience | production should isolate service ownership with separate schemas/DBs and status events/read models |
| Logging provider adapters | repository works without vendor accounts or secrets | does not send real SMS/push/email until a provider adapter is supplied |

## Failure scenarios

### API crashes after DB commit
The outbox row remains `PENDING`; another poller publishes it later.

### Kafka redelivers a delivery command
The `(notification_id, channel)` unique record is checked. `SENT` commands become no-ops.

### Worker crashes after provider call but before persistence
This is the classic external-side-effect ambiguity. A retry may contact the provider again. In production, pass the notification/channel identifier as the provider idempotency key when the provider supports it. The project intentionally documents this boundary rather than claiming impossible global exactly-once semantics.

### Redis is temporarily unavailable
Current implementation fails the delivery attempt and lets Kafka retry. A production policy could instead fail open for critical transactional notifications and fail closed for bulk/promotional traffic.

### Provider keeps failing
Exponential retries are exhausted and the record moves to DLT. A status event marks the notification `FAILED` for operator/user visibility.

## Production topic configuration

The repository creates low-replication topics for one-machine local development. In a real multi-broker cluster, pre-provision topics with an appropriate replication factor (commonly 3), retention policy, min ISR, quotas and ACLs, and disable application-side topic creation where platform teams own Kafka configuration.
